/*
 * VocabularyPronunciationServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：PronunciationAccentInvalidException、PronunciationDisabledException、PronunciationUpstreamException、PronunciationWordInvalidException、VocabularyPronunciationProperties、VocabularyPronunciationService、PronunciationAudioSource。
 * 阅读入口：audio（音频处理入口）。
 */

package com.starrainnotes.english.vocabulary.service.impl;

import com.starrainnotes.english.vocabulary.exception.PronunciationAccentInvalidException;
import com.starrainnotes.english.vocabulary.exception.PronunciationDisabledException;
import com.starrainnotes.english.vocabulary.exception.PronunciationUpstreamException;
import com.starrainnotes.english.vocabulary.exception.PronunciationWordInvalidException;
import com.starrainnotes.english.vocabulary.properties.VocabularyPronunciationProperties;
import com.starrainnotes.english.vocabulary.service.VocabularyPronunciationService;
import com.starrainnotes.english.vocabulary.storage.PronunciationAudioSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
 * 发音代理实现：校验 -> 查缓存 -> 取上游 -> 写缓存。
 *
 * 缓存以 sha256(word|accent) 命名，内容一旦落盘就不可变（同一个词同一口音的音频不会变），
 * 因此响应可以给浏览器长缓存。缓存读写都是尽力而为：读失败或写失败只影响命中率，
 * 不影响本次响应。
 */
/**
 * 词汇Pronunciation业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class VocabularyPronunciationServiceImpl implements VocabularyPronunciationService {

    /**
     * 当前类的日志记录器；日志参数不能包含密码或会话令牌。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyPronunciationServiceImpl.class);

    /* 单个英文词：字母开头，可含内部撇号与连字符 */
    /**
     * 模式。
     */
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z][A-Za-z'\\-]*");
    /**
     * LENGTH。
     * 初始值为 <code>64</code>。
     */
    private static final int MAX_WORD_LENGTH = 64;
    /**
     * US。
     */
    private static final String ACCENT_US = "US";
    /**
     * UK。
     */
    private static final String ACCENT_UK = "UK";

    /**
     * 音频来源。
     */
    private final PronunciationAudioSource audioSource;
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>VocabularyPronunciationProperties</code>。
     */
    private final VocabularyPronunciationProperties properties;
    /**
     * 词汇PronunciationHTTP客户端。
     */
    private final HttpClient vocabularyPronunciationHttpClient;

    /**
     * 音频处理入口。
     * 主要协作调用：<code>audioSource.enabled</code>、<code>audioSource.audioUrl</code>。
     * 实现中的显式失败类型包括 <code>PronunciationDisabledException</code>；具体触发条件见对应分支。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    @Override
    public byte[] audio(String word, String accent) {
        String clean = normalizeWord(word);
        String normalizedAccent = normalizeAccent(accent);
        if (!audioSource.enabled()) {
            throw new PronunciationDisabledException();
        }
        Optional<String> url = audioSource.audioUrl(clean, normalizedAccent);
        if (url.isEmpty()) {
            throw new PronunciationDisabledException();
        }
        Path cache = cachePath(clean, normalizedAccent);
        byte[] cached = readCache(cache);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = fetch(url.get());
        writeCache(cache, bytes);
        return bytes;
    }

    /**
     * fetch处理入口。
     * 主要协作调用：<code>properties.getRequestTimeoutSeconds</code>、<code>vocabularyPronunciationHttpClient.send</code>。
     * 实现中的显式失败类型包括 <code>PronunciationUpstreamException</code>；具体触发条件见对应分支。
     *
     * @param url 地址；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    private byte[] fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(1, properties.getRequestTimeoutSeconds())))
                .header("User-Agent", "Mozilla/5.0 (StarRainNotes v2 vocabulary pronunciation proxy)")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = vocabularyPronunciationHttpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
                throw new PronunciationUpstreamException("发音服务返回 " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new PronunciationUpstreamException("无法连接发音服务", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PronunciationUpstreamException("发音请求被中断", exception);
        }
    }

    /**
     * 缓存路径处理入口。
     * 主要协作调用：<code>properties.getCacheDir</code>。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 类型为 <code>Path</code> 的处理结果
     */
    private Path cachePath(String word, String accent) {
        return Path.of(properties.getCacheDir()).resolve(sha256(word + "|" + accent) + ".mp3");
    }

    /**
     * 读取缓存。
     * 关键处理步骤调用 <code>Files.isRegularFile</code>、<code>Files.size</code>、<code>Files.readAllBytes</code>。
     *
     * @param path 路径；类型为 <code>Path</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    private byte[] readCache(Path path) {
        try {
            if (Files.isRegularFile(path) && Files.size(path) > 0) {
                return Files.readAllBytes(path);
            }
        } catch (IOException exception) {
            LOGGER.warn("发音缓存读取失败，改为请求上游: {}", path, exception);
        }
        return null;
    }

    /**
     * 写入缓存。
     * 关键处理步骤调用 <code>Files.createDirectories</code>、<code>path.getParent</code>、<code>Files.write</code>。
     *
     * @param path 路径；类型为 <code>Path</code>
     * @param bytes 字节；类型为 <code>byte[]</code>
     */
    private void writeCache(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException exception) {
            LOGGER.warn("发音缓存写入失败，本次响应不受影响: {}", path, exception);
        }
    }

    /**
     * 规范化单词。
     * 主要协作调用：<code>WORD_PATTERN.matcher</code>。
     * 实现中的显式失败类型包括 <code>PronunciationWordInvalidException</code>；具体触发条件见对应分支。
     *
     * @param word 单词；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String normalizeWord(String word) {
        if (word == null || word.isBlank()) {
            throw new PronunciationWordInvalidException("缺少 word 参数");
        }
        String trimmed = word.trim();
        if (trimmed.length() > MAX_WORD_LENGTH) {
            throw new PronunciationWordInvalidException("单词长度不能超过 " + MAX_WORD_LENGTH + " 个字符");
        }
        if (!WORD_PATTERN.matcher(trimmed).matches()) {
            throw new PronunciationWordInvalidException("只支持单个英文单词");
        }
        return trimmed;
    }

    /**
     * 规范化Accent。
     * 主要协作调用：<code>properties.getDefaultAccent</code>、<code>ACCENT_US.equals</code>、<code>ACCENT_UK.equals</code>。
     * 实现中的显式失败类型包括 <code>PronunciationAccentInvalidException</code>；具体触发条件见对应分支。
     *
     * @param accent accent；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String normalizeAccent(String accent) {
        if (accent == null || accent.isBlank()) {
            return normalizeAccent(properties.getDefaultAccent());
        }
        String upper = accent.trim().toUpperCase(Locale.ROOT);
        if (!ACCENT_US.equals(upper) && !ACCENT_UK.equals(upper)) {
            throw new PronunciationAccentInvalidException();
        }
        return upper;
    }

    /**
     * 文件内容的 SHA-256 摘要处理入口。
     * 关键处理步骤调用 <code>MessageDigest.getInstance</code>、<code>HexFormat.of</code>、<code>digest.digest</code>、<code>value.getBytes</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}

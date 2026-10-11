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
@Service
@RequiredArgsConstructor
public class VocabularyPronunciationServiceImpl implements VocabularyPronunciationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyPronunciationServiceImpl.class);

    /* 单个英文词：字母开头，可含内部撇号与连字符 */
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z][A-Za-z'\\-]*");
    private static final int MAX_WORD_LENGTH = 64;
    private static final String ACCENT_US = "US";
    private static final String ACCENT_UK = "UK";

    private final PronunciationAudioSource audioSource;
    private final VocabularyPronunciationProperties properties;
    private final HttpClient vocabularyPronunciationHttpClient;

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

    private Path cachePath(String word, String accent) {
        return Path.of(properties.getCacheDir()).resolve(sha256(word + "|" + accent) + ".mp3");
    }

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

    private void writeCache(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException exception) {
            LOGGER.warn("发音缓存写入失败，本次响应不受影响: {}", path, exception);
        }
    }

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

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}

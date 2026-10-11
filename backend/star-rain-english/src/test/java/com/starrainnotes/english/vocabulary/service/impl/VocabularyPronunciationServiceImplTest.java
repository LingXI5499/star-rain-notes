/*
 * VocabularyPronunciationServiceImplTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：PronunciationAccentInvalidException、PronunciationDisabledException、PronunciationWordInvalidException、VocabularyPronunciationProperties、ConfigurablePronunciationAudioSource。
 * 阅读入口：properties（本模块的外部配置属性处理入口）；service（业务服务处理入口）；plainEnglishWordIsAccepted（纯文本英语单词是否已接受处理入口）；phraseWithSpacesIsRejectedAsBadRequest（phrase包含Spaces是否已拒绝AsBad请求处理入口）。
 */

package com.starrainnotes.english.vocabulary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.english.vocabulary.exception.PronunciationAccentInvalidException;
import com.starrainnotes.english.vocabulary.exception.PronunciationDisabledException;
import com.starrainnotes.english.vocabulary.exception.PronunciationWordInvalidException;
import com.starrainnotes.english.vocabulary.properties.VocabularyPronunciationProperties;
import com.starrainnotes.english.vocabulary.storage.impl.ConfigurablePronunciationAudioSource;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/*
 * 发音代理的校验、开关与缓存行为。
 *
 * 这些用例刻意不触网：命中缓存或提供方关闭时都在发起请求之前返回，
 * 因此用真实的 HttpClient 也不会产生外部依赖。
 */
/**
 * 词汇Pronunciation业务服务实现测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class VocabularyPronunciationServiceImplTest {

    /**
     * 缓存Dir。
     */
    @TempDir Path cacheDir;

    /**
     * 本模块的外部配置属性处理入口。
     * 主要协作调用：<code>cacheDir.toString</code>。
     *
     * @param provider 提供方；类型为 <code>String</code>
     * @param enabled 已启用；类型为 <code>boolean</code>
     * @return 类型为 <code>VocabularyPronunciationProperties</code> 的处理结果
     */
    private VocabularyPronunciationProperties properties(String provider, boolean enabled) {
        VocabularyPronunciationProperties properties = new VocabularyPronunciationProperties();
        properties.setProvider(provider);
        properties.setEnabled(enabled);
        properties.setCacheDir(cacheDir.toString());
        return properties;
    }

    /**
     * 业务服务处理入口。
     * 关键处理步骤调用 <code>HttpClient.newHttpClient</code>。
     *
     * @param properties 本模块的外部配置属性；类型为 <code>VocabularyPronunciationProperties</code>
     * @return 类型为 <code>VocabularyPronunciationServiceImpl</code> 的处理结果
     */
    private VocabularyPronunciationServiceImpl service(VocabularyPronunciationProperties properties) {
        return new VocabularyPronunciationServiceImpl(
                new ConfigurablePronunciationAudioSource(properties), properties, HttpClient.newHttpClient());
    }

    /**
     * 纯文本英语单词是否已接受处理入口。
     * 关键处理步骤调用 <code>properties</code>。
     */
    @Test
    void plainEnglishWordIsAccepted() {
        assertThat(new ConfigurablePronunciationAudioSource(properties("youdao", true))
                .audioUrl("hello", "US")).isPresent();
    }

    /**
     * phrase包含Spaces是否已拒绝AsBad请求处理入口。
     * 关键处理步骤调用 <code>service</code>、<code>properties</code>。
     */
    @Test
    void phraseWithSpacesIsRejectedAsBadRequest() {
        assertThatThrownBy(() -> service(properties("youdao", true)).audio("hello world", "US"))
                .isInstanceOf(PronunciationWordInvalidException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationWordInvalidException) error).getStatus()).isEqualTo(400));
    }

    /**
     * blank及OverlongWordsAre已拒绝处理入口。
     * 关键处理步骤调用 <code>service</code>、<code>properties</code>、<code>service.audio</code>。
     */
    @Test
    void blankAndOverlongWordsAreRejected() {
        VocabularyPronunciationServiceImpl service = service(properties("youdao", true));

        assertThatThrownBy(() -> service.audio("  ", "US"))
                .isInstanceOf(PronunciationWordInvalidException.class);
        assertThatThrownBy(() -> service.audio("a".repeat(65), "US"))
                .isInstanceOf(PronunciationWordInvalidException.class);
    }

    /**
     * unknownAccent是否已拒绝处理入口。
     * 关键处理步骤调用 <code>service</code>、<code>properties</code>。
     */
    @Test
    void unknownAccentIsRejected() {
        assertThatThrownBy(() -> service(properties("youdao", true)).audio("hello", "AU"))
                .isInstanceOf(PronunciationAccentInvalidException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationAccentInvalidException) error).getStatus()).isEqualTo(400));
    }

    /**
     * 已禁用提供方Returns不存在So客户端FallsBack目标BrowserSpeech处理入口。
     * 关键处理步骤调用 <code>service</code>、<code>properties</code>。
     */
    @Test
    void disabledProviderReturnsNotFoundSoClientFallsBackToBrowserSpeech() {
        assertThatThrownBy(() -> service(properties("disabled", true)).audio("hello", "US"))
                .isInstanceOf(PronunciationDisabledException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationDisabledException) error).getStatus()).isEqualTo(404));
    }

    /**
     * globalSwitchOffAlsoReturns不存在处理入口。
     * 关键处理步骤调用 <code>service</code>、<code>properties</code>。
     */
    @Test
    void globalSwitchOffAlsoReturnsNotFound() {
        assertThatThrownBy(() -> service(properties("youdao", false)).audio("hello", "US"))
                .isInstanceOf(PronunciationDisabledException.class);
    }

    /**
     * cached音频是否Served不含CallingUpstream处理入口。
     * 主要协作调用：<code>cacheDir.resolve</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void cachedAudioIsServedWithoutCallingUpstream() throws Exception {
        byte[] audio = "fake-mp3-bytes".getBytes(StandardCharsets.UTF_8);
        Path cached = cacheDir.resolve(sha256("hello|US") + ".mp3");
        Files.createDirectories(cacheDir);
        Files.write(cached, audio);

        byte[] result = service(properties("youdao", true)).audio(" hello ", "us");

        assertThat(result).isEqualTo(audio);
    }

    /**
     * absent缓存Directory是否创建触发Demand处理入口。
     * 主要协作调用：<code>cacheDir.resolve</code>。
     */
    @Test
    void absentCacheDirectoryIsCreatedOnDemand() {
        Path missing = cacheDir.resolve("nested/cache");
        VocabularyPronunciationProperties properties = properties("youdao", true);
        properties.setCacheDir(missing.toString());

        assertThat(missing).doesNotExist();
        assertThat(new ConfigurablePronunciationAudioSource(properties).enabled()).isTrue();
    }

    /**
     * 文件内容的 SHA-256 摘要处理入口。
     * 关键处理步骤调用 <code>MessageDigest.getInstance</code>、<code>HexFormat.of</code>、<code>digest.digest</code>、<code>value.getBytes</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

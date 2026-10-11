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
class VocabularyPronunciationServiceImplTest {

    @TempDir Path cacheDir;

    private VocabularyPronunciationProperties properties(String provider, boolean enabled) {
        VocabularyPronunciationProperties properties = new VocabularyPronunciationProperties();
        properties.setProvider(provider);
        properties.setEnabled(enabled);
        properties.setCacheDir(cacheDir.toString());
        return properties;
    }

    private VocabularyPronunciationServiceImpl service(VocabularyPronunciationProperties properties) {
        return new VocabularyPronunciationServiceImpl(
                new ConfigurablePronunciationAudioSource(properties), properties, HttpClient.newHttpClient());
    }

    @Test
    void plainEnglishWordIsAccepted() {
        assertThat(new ConfigurablePronunciationAudioSource(properties("youdao", true))
                .audioUrl("hello", "US")).isPresent();
    }

    @Test
    void phraseWithSpacesIsRejectedAsBadRequest() {
        assertThatThrownBy(() -> service(properties("youdao", true)).audio("hello world", "US"))
                .isInstanceOf(PronunciationWordInvalidException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationWordInvalidException) error).getStatus()).isEqualTo(400));
    }

    @Test
    void blankAndOverlongWordsAreRejected() {
        VocabularyPronunciationServiceImpl service = service(properties("youdao", true));

        assertThatThrownBy(() -> service.audio("  ", "US"))
                .isInstanceOf(PronunciationWordInvalidException.class);
        assertThatThrownBy(() -> service.audio("a".repeat(65), "US"))
                .isInstanceOf(PronunciationWordInvalidException.class);
    }

    @Test
    void unknownAccentIsRejected() {
        assertThatThrownBy(() -> service(properties("youdao", true)).audio("hello", "AU"))
                .isInstanceOf(PronunciationAccentInvalidException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationAccentInvalidException) error).getStatus()).isEqualTo(400));
    }

    @Test
    void disabledProviderReturnsNotFoundSoClientFallsBackToBrowserSpeech() {
        assertThatThrownBy(() -> service(properties("disabled", true)).audio("hello", "US"))
                .isInstanceOf(PronunciationDisabledException.class)
                .satisfies(error -> assertThat(
                        ((PronunciationDisabledException) error).getStatus()).isEqualTo(404));
    }

    @Test
    void globalSwitchOffAlsoReturnsNotFound() {
        assertThatThrownBy(() -> service(properties("youdao", false)).audio("hello", "US"))
                .isInstanceOf(PronunciationDisabledException.class);
    }

    @Test
    void cachedAudioIsServedWithoutCallingUpstream() throws Exception {
        byte[] audio = "fake-mp3-bytes".getBytes(StandardCharsets.UTF_8);
        Path cached = cacheDir.resolve(sha256("hello|US") + ".mp3");
        Files.createDirectories(cacheDir);
        Files.write(cached, audio);

        byte[] result = service(properties("youdao", true)).audio(" hello ", "us");

        assertThat(result).isEqualTo(audio);
    }

    @Test
    void absentCacheDirectoryIsCreatedOnDemand() {
        Path missing = cacheDir.resolve("nested/cache");
        VocabularyPronunciationProperties properties = properties("youdao", true);
        properties.setCacheDir(missing.toString());

        assertThat(missing).doesNotExist();
        assertThat(new ConfigurablePronunciationAudioSource(properties).enabled()).isTrue();
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

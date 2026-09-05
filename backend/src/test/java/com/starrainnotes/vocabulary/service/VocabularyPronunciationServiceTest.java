package com.starrainnotes.vocabulary.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.vocabulary.config.VocabularyProperties;
import com.starrainnotes.vocabulary.provider.PronunciationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the pronunciation proxy/cache logic with a mocked {@link HttpClient}.
 * Pure unit test — no Spring context, no database, no real network.
 */
class VocabularyPronunciationServiceTest {

    private static final byte[] AUDIO = {0x49, 0x44, 0x33, 0x04};

    @TempDir
    Path cacheDir;

    private PronunciationProvider provider;
    private HttpClient http;
    private HttpResponse<byte[]> response;
    private VocabularyPronunciationService service;

    @BeforeEach
    void setUp() {
        provider = mock(PronunciationProvider.class);
        http = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        VocabularyProperties props = new VocabularyProperties(
                new VocabularyProperties.Providers("youdao"),
                new VocabularyProperties.Youdao("https://dict.youdao.com/dictvoice", "0", "1"),
                new VocabularyProperties.Pronunciation("US", cacheDir.toString()));
        service = new VocabularyPronunciationService(provider, props, http);
    }

    @Test
    void fetchesAndReturnsBytes() throws Exception {
        when(provider.pronunciationUrl("hello", "US"))
                .thenReturn(Optional.of("https://dict.youdao.com/dictvoice?type=0&audio=hello"));
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(AUDIO);
        doReturn(response).when(http).send(any(HttpRequest.class), any());

        byte[] result = service.audio("hello", "US");

        assertThat(result).isEqualTo(AUDIO);
        // Cached on disk for the next call.
        assertThat(Files.walk(cacheDir).anyMatch(p -> p.toString().endsWith(".mp3"))).isTrue();
    }

    @Test
    void secondCallServedFromCacheWithoutUpstream() throws Exception {
        when(provider.pronunciationUrl("hello", "US"))
                .thenReturn(Optional.of("https://dict.youdao.com/dictvoice?type=0&audio=hello"));
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(AUDIO);
        doReturn(response).when(http).send(any(HttpRequest.class), any());

        service.audio("hello", "US");
        byte[] second = service.audio("hello", "US");

        assertThat(second).isEqualTo(AUDIO);
        verify(http).send(any(HttpRequest.class), any());  // exactly one upstream call
    }

    @Test
    void disabledProviderReturnsNotFound() {
        when(provider.pronunciationUrl("hello", "US")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.audio("hello", "US"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(404));
    }

    @Test
    void invalidWordRejected() {
        assertThatThrownBy(() -> service.audio("hello world", "US"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(400));
        assertThatThrownBy(() -> service.audio("", "US"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void invalidAccentRejected() {
        assertThatThrownBy(() -> service.audio("hello", "FR"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void upstreamBadStatusGivesBadGateway() throws Exception {
        when(provider.pronunciationUrl("hello", "US"))
                .thenReturn(Optional.of("https://dict.youdao.com/dictvoice?type=0&audio=hello"));
        when(response.statusCode()).thenReturn(500);
        doReturn(response).when(http).send(any(HttpRequest.class), any());

        assertThatThrownBy(() -> service.audio("hello", "US"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(502));
    }
}

package com.starrainnotes.vocabulary;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the public pronunciation endpoint wiring with the Youdao provider
 * enabled and the outbound {@link HttpClient} mocked (no real upstream call).
 * Requires the test MySQL profile (full context boots), so it runs locally
 * where the {@code star_rain_notes_test} database is available.
 */
class VocabularyPronunciationIntegrationTest extends AbstractAuthIntegrationTest {

    @MockBean
    private HttpClient httpClient;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.vocabulary.providers.pronunciation", () -> "youdao");
        registry.add("app.vocabulary.youdao.dict-voice-base-url", () -> "https://dict.youdao.com/dictvoice");
        registry.add("app.vocabulary.pronunciation.cache-dir", () -> tempDir().toString());
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("pronunciation-cache-it");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void servesProxiedAudioWithCacheControl() throws Exception {
        byte[] audio = "ID3".getBytes(StandardCharsets.UTF_8);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(audio);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        mockMvc.perform(get("/api/v1/public/vocabulary/pronunciation")
                        .param("word", "hello").param("accent", "US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=2592000, immutable"))
                .andExpect(content().bytes(audio));
    }

    @Test
    void servedFromDiskCacheOnRepeat() throws Exception {
        byte[] audio = "ID3".getBytes(StandardCharsets.UTF_8);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(audio);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        mockMvc.perform(get("/api/v1/public/vocabulary/pronunciation").param("word", "hello"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/vocabulary/pronunciation").param("word", "hello"))
                .andExpect(status().isOk());
    }
}

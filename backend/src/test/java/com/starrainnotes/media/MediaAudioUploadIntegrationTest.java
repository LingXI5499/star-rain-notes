package com.starrainnotes.media;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 audio-media rules (方案 §13.1): MP3 / M4A / OGG accepted only when
 * the declared MIME matches AND the container signature is real; fake
 * extensions, mismatched MIME and wrong signatures are rejected.
 */
class MediaAudioUploadIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private static final Path TEST_UPLOADS = Path.of("target/test-uploads").toAbsolutePath().normalize();

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() throws IOException {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        resetMedia();
    }

    @AfterEach
    void cleanUp() throws IOException {
        jdbc.update("DELETE FROM admin_user");
        resetMedia();
    }

    private void resetMedia() throws IOException {
        jdbc.update("DELETE FROM media_asset");
        if (Files.exists(TEST_UPLOADS)) {
            try (var walk = Files.walk(TEST_UPLOADS)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best effort
                    }
                });
            }
        }
    }

    @Test
    void validAudioContainersAreStoredAsAudio() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(withCsrf(multipartUpload("clip.mp3", "audio/mpeg", mp3()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("AUDIO"))
                .andExpect(jsonPath("$.extension").value("mp3"))
                .andExpect(jsonPath("$.width").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(withCsrf(multipartUpload("track.m4a", "audio/mp4", m4a()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("AUDIO"))
                .andExpect(jsonPath("$.extension").value("m4a"));

        mockMvc.perform(withCsrf(multipartUpload("voice.ogg", "audio/ogg", ogg()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("AUDIO"))
                .andExpect(jsonPath("$.extension").value("ogg"));
    }

    @Test
    void nonAudioBytesDisguisedAsMp3AreRejected() throws Exception {
        MockHttpSession session = loginSession();
        // PNG magic disguised as .mp3 -> signature mismatch
        mockMvc.perform(withCsrf(multipartUpload("fake.mp3", "audio/mpeg", pngBytes()), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        // arbitrary garbage
        byte[] junk = "not an audio file at all".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        mockMvc.perform(withCsrf(multipartUpload("junk.mp3", "audio/mpeg", junk), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void declaredMimeMismatchIsRejected() throws Exception {
        MockHttpSession session = loginSession();
        // mp3 bytes but declared as mp4
        mockMvc.perform(withCsrf(multipartUpload("clip.mp3", "audio/mp4", mp3()), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        // image MIME on an audio extension
        mockMvc.perform(withCsrf(multipartUpload("clip.ogg", "image/png", ogg()), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unsupportedExtensionStillRejected() throws Exception {
        MockHttpSession session = loginSession();
        byte[] wav = "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        mockMvc.perform(withCsrf(multipartUpload("wave.wav", "audio/wav", wav), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------

    private MockMultipartHttpServletRequestBuilder multipartUpload(String filename, String contentType, byte[] bytes) {
        return multipart("/api/v1/admin/media-assets")
                .file(new MockMultipartFile("file", filename, contentType, bytes));
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private static byte[] mp3() {
        byte[] bytes = new byte[]{0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        return bytes;
    }

    private static byte[] m4a() {
        byte[] bytes = new byte[12];
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x18;
        byte[] ftyp = "ftyp".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(ftyp, 0, bytes, 4, 4);
        return bytes;
    }

    private static byte[] ogg() {
        byte[] bytes = "OggS".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] full = new byte[16];
        System.arraycopy(bytes, 0, full, 0, 4);
        return full;
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    }
}

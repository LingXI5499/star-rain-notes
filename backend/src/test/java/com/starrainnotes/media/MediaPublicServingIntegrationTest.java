package com.starrainnotes.media;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-012 P0 fix: uploaded media must be publicly readable at its
 * {@code /uploads/...} URL (SecurityConfig whitelist + MediaResourceConfig
 * static handler), and the handler must reject path traversal.
 */
class MediaPublicServingIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private static final Path TEST_UPLOADS = Path.of("target/test-uploads").toAbsolutePath().normalize();

    private static final byte[] PNG_BYTES = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private String upload(MockHttpSession session) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/v1/admin/media-assets")
                .file(new MockMultipartFile("file", "smoke.png", "image/png", PNG_BYTES));
        MvcResult result = mockMvc.perform(withCsrf(builder, csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("publicUrl").asText();
    }

    @Test
    void publicMediaUrlIsReadableAnonymously() throws Exception {
        MockHttpSession session = loginSession();
        String publicUrl = upload(session);

        assertThat(publicUrl).startsWith("/uploads/");

        mockMvc.perform(get(publicUrl))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void mediaUrlCarriesImmutableCacheControl() throws Exception {
        MockHttpSession session = loginSession();
        String publicUrl = upload(session);

        mockMvc.perform(get(publicUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=2592000"));
    }

    @Test
    void pathTraversalOutsideUploadsIsRejected() throws Exception {
        mockMvc.perform(get("/uploads/../../application.yml"))
                .andExpect(status().is4xxClientError());
    }
}

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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-009 — secure media management: allowlist + declared-MIME + magic bytes +
 * decodability, UUID/year-month relative storage, size limits, failure
 * compensation and FK SET NULL on delete.
 */
class MediaAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private static final Path TEST_UPLOADS = Path.of("target/test-uploads").toAbsolutePath().normalize();

    private static final String PNG_1X1_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

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
        resetSiteSettings();
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

    private void resetSiteSettings() {
        jdbc.update("""
                UPDATE site_setting
                SET site_name = '星雨笔录', tagline = 'Knowledge · Code · Growth',
                    site_url = NULL, footer_text = NULL, github_url = NULL,
                    default_seo_description = '个人知识、技术教程、博客与项目作品记录。',
                    timezone = 'Asia/Shanghai', logo_media_id = NULL, favicon_media_id = NULL
                WHERE id = 1
                """);
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

    private MockMultipartHttpServletRequestBuilder multipartUpload(String filename, String contentType, byte[] bytes) {
        return multipart("/api/v1/admin/media-assets")
                .file(new MockMultipartFile("file", filename, contentType, bytes));
    }

    private Long upload(MockHttpSession session, String filename, String contentType, byte[] bytes) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(multipartUpload(filename, contentType, bytes), csrf(session))
                        .session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ---------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------

    private static byte[] png1x1() {
        return Base64.getDecoder().decode(PNG_1X1_BASE64);
    }

    private static byte[] jpeg2x3() throws IOException {
        BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private static byte[] webp1x1() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.writeBytes(new byte[]{0, 0, 0, 0});
        out.writeBytes("WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.writeBytes("VP8L".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        byte[] payload = {0x2F, 0, 0, 0, 0, 0, 0, 0};
        out.writeBytes(new byte[]{(byte) payload.length, 0, 0, 0});
        out.writeBytes(payload);
        byte[] all = out.toByteArray();
        int size = all.length - 8;
        all[4] = (byte) size;
        all[5] = (byte) (size >> 8);
        all[6] = (byte) (size >> 16);
        all[7] = (byte) (size >> 24);
        return all;
    }

    private static byte[] pdf() {
        return "%PDF-1.4\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    // ---------------------------------------------------------------
    // valid uploads
    // ---------------------------------------------------------------

    @Test
    void uploadValidPngStoresRelativePathAndDimensions() throws Exception {
        MockHttpSession session = loginSession();
        String body = mockMvc.perform(withCsrf(multipartUpload("portrait.png", "image/png", png1x1()), csrf(session))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("IMAGE"))
                .andExpect(jsonPath("$.width").value(1))
                .andExpect(jsonPath("$.height").value(1))
                .andReturn().getResponse().getContentAsString();

        Long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
        String storagePath = jdbc.queryForObject("SELECT storage_path FROM media_asset WHERE id = ?", String.class, id);
        String publicUrl = jdbc.queryForObject("SELECT public_url FROM media_asset WHERE id = ?", String.class, id);

        // relative year/month storage key, no absolute/root path
        assertThat(storagePath).matches("\\d{4}/\\d{2}/[0-9a-f-]+\\.png");
        assertThat(storagePath).doesNotStartWith("/").doesNotStartWith("\\").doesNotContain(":");
        assertThat(publicUrl).isEqualTo("/uploads/" + storagePath);
        assertThat(Files.exists(TEST_UPLOADS.resolve(storagePath))).isTrue();
    }

    @Test
    void uploadValidJpegWebpAndPdf() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(withCsrf(multipartUpload("photo.jpg", "image/jpeg", jpeg2x3()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("IMAGE"))
                .andExpect(jsonPath("$.width").value(2))
                .andExpect(jsonPath("$.height").value(3));

        mockMvc.perform(withCsrf(multipartUpload("pic.webp", "image/webp", webp1x1()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("IMAGE"))
                .andExpect(jsonPath("$.width").value(1))
                .andExpect(jsonPath("$.height").value(1));

        mockMvc.perform(withCsrf(multipartUpload("resume.pdf", "application/pdf", pdf()), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("DOCUMENT"))
                .andExpect(jsonPath("$.width").value(org.hamcrest.Matchers.nullValue()));
    }

    // ---------------------------------------------------------------
    // rejection
    // ---------------------------------------------------------------

    @Test
    void renamedExecutableRejected() throws Exception {
        MockHttpSession session = loginSession();
        // PNG bytes renamed as .exe -> extension not allowlisted
        mockMvc.perform(withCsrf(multipartUpload("evil.exe", "image/png", png1x1()), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        // executable MZ bytes renamed as .png -> signature mismatch
        byte[] mz = new byte[]{0x4D, 0x5A, 0x50, 0x00, 0x02, 0x00, 0x00, 0x00, 0x04, 0x00};
        mockMvc.perform(withCsrf(multipartUpload("virus.png", "image/png", mz), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void forbiddenExtensionsRejected() throws Exception {
        MockHttpSession session = loginSession();
        byte[] bytes = png1x1();
        for (String ext : new String[]{"gif", "svg", "doc", "zip", "html", "js"}) {
            mockMvc.perform(withCsrf(multipartUpload("x." + ext, "image/png", bytes), csrf(session)).session(session))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }
    }

    @Test
    void oversizeRejected() throws Exception {
        MockHttpSession session = loginSession();
        byte[] bigImage = new byte[11 * 1024 * 1024];
        bigImage[0] = (byte) 0x89;
        mockMvc.perform(withCsrf(multipartUpload("big.png", "image/png", bigImage), csrf(session)).session(session))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

        byte[] bigPdf = new byte[21 * 1024 * 1024];
        mockMvc.perform(withCsrf(multipartUpload("big.pdf", "application/pdf", bigPdf), csrf(session)).session(session))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void emptyFileRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(multipartUpload("empty.png", "image/png", new byte[0]), csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
    }

    @Test
    void declaredMimeMismatchRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(multipartUpload("x.png", "image/jpeg", png1x1()), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void corruptImageRejected() throws Exception {
        MockHttpSession session = loginSession();
        // valid PNG magic but garbage body -> not decodable by ImageIO
        byte[] corrupt = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02};
        mockMvc.perform(withCsrf(multipartUpload("broken.png", "image/png", corrupt), csrf(session)).session(session))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---------------------------------------------------------------
    // media rules on other modules
    // ---------------------------------------------------------------

    @Test
    void pdfCannotBeUsedAsCoverOrAvatar() throws Exception {
        MockHttpSession session = loginSession();
        Long pdfId = upload(session, "resume.pdf", "application/pdf", pdf());

        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Asia/Shanghai\",\"logoMediaId\":" + pdfId + "}"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"avatarMediaId\":" + pdfId + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void imageCannotBeUsedAsResume() throws Exception {
        MockHttpSession session = loginSession();
        Long imageId = upload(session, "photo.png", "image/png", png1x1());

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"resumeMediaId\":" + imageId + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));
    }

    // ---------------------------------------------------------------
    // list / delete
    // ---------------------------------------------------------------

    @Test
    void listSearchesAndFilters() throws Exception {
        MockHttpSession session = loginSession();
        upload(session, "portrait.png", "image/png", png1x1());
        upload(session, "resume.pdf", "application/pdf", pdf());

        mockMvc.perform(get("/api/v1/admin/media-assets").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/api/v1/admin/media-assets").param("q", "resume").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].originalName").value("resume.pdf"));

        mockMvc.perform(get("/api/v1/admin/media-assets").param("assetType", "IMAGE").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].assetType").value("IMAGE"));
    }

    @Test
    void deleteRemovesRowSetsNullAndDeletesFile() throws Exception {
        MockHttpSession session = loginSession();
        Long imageId = upload(session, "logo.png", "image/png", png1x1());
        String storagePath = jdbc.queryForObject("SELECT storage_path FROM media_asset WHERE id = ?", String.class, imageId);

        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Asia/Shanghai\",\"logoMediaId\":" + imageId + "}"),
                        csrf(session)).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(withCsrf(delete("/api/v1/admin/media-assets/" + imageId), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        Integer logo = jdbc.queryForObject("SELECT logo_media_id FROM site_setting WHERE id = 1", Integer.class);
        assertThat(logo).isNull();
        assertThat(Files.exists(TEST_UPLOADS.resolve(storagePath))).isFalse();
    }

    @Test
    void unauthenticatedAndCsrfRequired() throws Exception {
        mockMvc.perform(get("/api/v1/admin/media-assets"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = loginSession();
        mockMvc.perform(multipartUpload("x.png", "image/png", png1x1()).session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }
}

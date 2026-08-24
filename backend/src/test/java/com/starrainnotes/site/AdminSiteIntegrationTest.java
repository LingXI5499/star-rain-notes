package com.starrainnotes.site;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-004 — admin dashboard and singleton site settings:
 * authentication required, empty-state and counts, explicit DTO update,
 * IMAGE-only media validation, timezone validation, CSRF required.
 */
class AdminSiteIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        cleanTables();
        resetSiteSettings();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        cleanTables();
        resetSiteSettings();
    }

    private void cleanTables() {
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
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

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String settingsBody() {
        return "{\"siteName\":\"星雨笔录\",\"tagline\":\"tag\",\"siteUrl\":null,\"footerText\":null,"
                + "\"githubUrl\":null,\"defaultSeoDescription\":\"seo\",\"timezone\":\"Asia/Shanghai\","
                + "\"logoMediaId\":null,\"faviconMediaId\":null}";
    }

    private Long insertCategory(String slug) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, slug);
        return lastId();
    }

    private Long insertTutorial(Long categoryId, String slug, String status, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, 'summary', ?, ?)
                """, categoryId, slug, slug, status, publishedAt);
        return lastId();
    }

    private Long insertChapter(Long tutorialId, String slug, String status, LocalDateTime publishedAt) {
        jdbc.update("INSERT INTO tutorial_node (tutorial_id, node_type, title) VALUES (?, 'GROUP', ?)",
                tutorialId, slug + " group");
        Long groupId = lastId();
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'CHAPTER', ?, ?, 'body', ?, ?)
                """, tutorialId, groupId, slug, slug, status, publishedAt);
        return lastId();
    }

    private Long insertBlog(String slug, String status) {
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown, publish_status)
                VALUES (?, ?, 'summary', 'body', ?)
                """, slug, slug, status);
        return lastId();
    }

    private Long insertPortfolio(String slug, String status) {
        jdbc.update("""
                INSERT INTO portfolio_project (title, slug, summary, tech_stack, body_markdown, publish_status)
                VALUES (?, ?, 'summary', '[]', 'body', ?)
                """, slug, slug, status);
        return lastId();
    }

    private Long insertMedia(String storedName, String assetType) {
        jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url)
                VALUES (?, ?, ?, ?, ?, 100, ?, ?)
                """, assetType, storedName + ".ext", storedName,
                assetType.equals("IMAGE") ? "image/webp" : "application/pdf",
                assetType.equals("IMAGE") ? "webp" : "pdf",
                "2026/08/" + storedName + ".webp", "/uploads/2026/08/" + storedName + ".webp");
        return lastId();
    }

    private Long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ---------------------------------------------------------------
    // dashboard
    // ---------------------------------------------------------------

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void dashboardEmptyState() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentCounts.tutorials").value(0))
                .andExpect(jsonPath("$.contentCounts.chapters").value(0))
                .andExpect(jsonPath("$.contentCounts.blogPosts").value(0))
                .andExpect(jsonPath("$.contentCounts.portfolioProjects").value(0))
                .andExpect(jsonPath("$.draftCounts.tutorials").value(0))
                .andExpect(jsonPath("$.draftCounts.blogPosts").value(0))
                .andExpect(jsonPath("$.recentContent").isEmpty());
    }

    @Test
    void dashboardCountsAndRecentContent() throws Exception {
        Long categoryId = insertCategory("dash-cat");
        Long t1 = insertTutorial(categoryId, "t1", "DRAFT", null);
        Long t2 = insertTutorial(categoryId, "t2", "PUBLISHED", LocalDateTime.of(2026, 7, 1, 0, 0));
        insertChapter(t1, "ch1", "DRAFT", null);
        insertChapter(t2, "ch2", "PUBLISHED", LocalDateTime.of(2026, 7, 1, 0, 0));
        insertBlog("b1", "DRAFT");
        insertPortfolio("p1", "DRAFT");

        // make t2 the most recently updated (explicit future timestamp beats real insert times)
        jdbc.update("UPDATE tutorial SET updated_at = '2030-01-01 00:00:00' WHERE id = ?", t2);

        MockHttpSession session = loginSession();
        String body = mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentCounts.tutorials").value(2))
                .andExpect(jsonPath("$.contentCounts.chapters").value(2))
                .andExpect(jsonPath("$.contentCounts.blogPosts").value(1))
                .andExpect(jsonPath("$.contentCounts.portfolioProjects").value(1))
                .andExpect(jsonPath("$.draftCounts.tutorials").value(1))
                .andExpect(jsonPath("$.draftCounts.chapters").value(1))
                .andExpect(jsonPath("$.draftCounts.blogPosts").value(1))
                .andExpect(jsonPath("$.draftCounts.portfolioProjects").value(1))
                .andExpect(jsonPath("$.recentContent[0].type").value("TUTORIAL"))
                .andExpect(jsonPath("$.recentContent[0].title").value("t2"))
                .andExpect(jsonPath("$.recentContent[0].publishStatus").value("PUBLISHED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("+08:00");
    }

    // ---------------------------------------------------------------
    // site settings
    // ---------------------------------------------------------------

    @Test
    void siteSettingsGetReturnsSingleton() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(get("/api/v1/admin/site-settings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.siteName").value("星雨笔录"))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"));
    }

    @Test
    void siteSettingsUpdateSucceeds() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = fetchCsrfTokenFor(session);
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"新名称\",\"tagline\":\"新标语\",\"siteUrl\":\"https://example.com\","
                                + "\"footerText\":\"脚注\",\"githubUrl\":\"https://github.com/x\","
                                + "\"defaultSeoDescription\":\"新的描述\",\"timezone\":\"UTC\","
                                + "\"logoMediaId\":null,\"faviconMediaId\":null}"), csrf)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("新名称"))
                .andExpect(jsonPath("$.tagline").value("新标语"))
                .andExpect(jsonPath("$.timezone").value("UTC"));

        String siteName = jdbc.queryForObject("SELECT site_name FROM site_setting WHERE id = 1", String.class);
        String timezone = jdbc.queryForObject("SELECT timezone FROM site_setting WHERE id = 1", String.class);
        assertThat(siteName).isEqualTo("新名称");
        assertThat(timezone).isEqualTo("UTC");
    }

    @Test
    void siteSettingsUpdateRequiresAuthentication() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(settingsBody()), csrf))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void siteSettingsUpdateRequiresCsrf() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(settingsBody())
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void siteSettingsUpdateRejectsNonImageMedia() throws Exception {
        Long documentId = insertMedia("doc-1", "DOCUMENT");
        Long imageId = insertMedia("img-1", "IMAGE");

        MockHttpSession session = loginSession();
        String csrf = fetchCsrfTokenFor(session);

        // DOCUMENT as logo rejected
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Asia/Shanghai\",\"logoMediaId\":" + documentId + "}"), csrf)
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));

        // IMAGE as favicon accepted
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Asia/Shanghai\",\"faviconMediaId\":" + imageId + "}"), csrf)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faviconMediaId").value(imageId));
    }

    @Test
    void siteSettingsUpdateRejectsMissingMedia() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = fetchCsrfTokenFor(session);
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Asia/Shanghai\",\"logoMediaId\":999999}"), csrf)
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_NOT_FOUND"));
    }

    @Test
    void siteSettingsUpdateRejectsInvalidTimezone() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = fetchCsrfTokenFor(session);
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"星雨笔录\",\"timezone\":\"Not/AZone\"}"), csrf)
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
    }

    @Test
    void siteSettingsUpdateValidatesBody() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = fetchCsrfTokenFor(session);
        mockMvc.perform(withCsrf(put("/api/v1/admin/site-settings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"\",\"timezone\":\"Asia/Shanghai\"}"), csrf)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String fetchCsrfTokenFor(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }
}

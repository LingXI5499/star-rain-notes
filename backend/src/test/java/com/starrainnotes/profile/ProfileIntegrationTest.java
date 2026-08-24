package com.starrainnotes.profile;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-008 — About: profile + selected content. Selected rules: ≤3 per type,
 * input order = display order, dedupe, all ids exist, Draft/Withdrawn allowed;
 * public outputs only PUBLISHED; withdraw keeps the relation; delete cascades.
 */
class ProfileIntegrationTest extends AbstractAuthIntegrationTest {

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
        cleanContent();
        resetProfile();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        cleanContent();
        resetProfile();
    }

    private void cleanContent() {
        jdbc.update("DELETE FROM profile_selected_content");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    private void resetProfile() {
        jdbc.update("""
                UPDATE profile
                SET display_name = NULL, headline = NULL, bio = NULL, avatar_media_id = NULL,
                    github_url = NULL, public_email = NULL, resume_media_id = NULL,
                    current_focus = JSON_ARRAY(), technical_direction_markdown = NULL, journey_markdown = NULL
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

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Long insertTutorial(String slug, String status) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES ('cat', 'cat-" + slug + "')");
        Long categoryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, 'summary', ?, ?)
                """, categoryId, slug, slug, status, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertBlog(String slug, String status) {
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'summary', 'body', ?, ?)
                """, slug, slug, status, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertPortfolio(String slug, String status) {
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, publish_status, project_status, published_at)
                VALUES (?, ?, 'summary', '[]', 'body', ?, 'DEVELOPING', ?)
                """, slug, slug, status, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertMedia(String storedName, String assetType, String mimeType, String extension) {
        jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url)
                VALUES (?, ?, ?, ?, ?, 100, ?, ?)
                """, assetType, storedName + ".ext", storedName, mimeType, extension,
                "2026/08/" + storedName + ".ext", "/uploads/2026/08/" + storedName + ".ext");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void selectContent(String tutorialsJson, String blogsJson, String projectsJson) throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(put("/api/v1/admin/about/selected-content")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"tutorialIds\":" + (tutorialsJson == null ? "[]" : tutorialsJson)
                                + ",\"blogPostIds\":" + (blogsJson == null ? "[]" : blogsJson)
                                + ",\"portfolioProjectIds\":" + (projectsJson == null ? "[]" : projectsJson) + "}"),
                        csrf(session)).session(session))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // profile
    // ---------------------------------------------------------------

    @Test
    void publicAboutEmptyState() throws Exception {
        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.selectedTutorials").isEmpty())
                .andExpect(jsonPath("$.selectedBlogs").isEmpty())
                .andExpect(jsonPath("$.selectedProjects").isEmpty());
    }

    @Test
    void updateProfileSucceedsWithFocus() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Ling\",\"headline\":\"Java Developer\",\"bio\":\"Bio\","
                                + "\"githubUrl\":\"https://github.com/x\",\"publicEmail\":\"a@b.com\","
                                + "\"currentFocus\":[\"Java\",\"Spring\"],\"technicalDirectionMarkdown\":\"## Tech\","
                                + "\"journeyMarkdown\":\"## Journey\"}"), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ling"))
                .andExpect(jsonPath("$.currentFocus[0]").value("Java"));

        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ling"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/x"));
    }

    @Test
    void avatarMustBeImage() throws Exception {
        MockHttpSession session = loginSession();
        Long documentId = insertMedia("doc", "DOCUMENT", "application/pdf", "pdf");
        Long imageId = insertMedia("img", "IMAGE", "image/webp", "webp");

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"avatarMediaId\":" + documentId + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"avatarMediaId\":" + imageId + "}"), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarMediaId").value(imageId));
    }

    @Test
    void resumeMustBePdfDocument() throws Exception {
        MockHttpSession session = loginSession();
        Long pdf = insertMedia("resume", "DOCUMENT", "application/pdf", "pdf");
        Long image = insertMedia("img2", "IMAGE", "image/webp", "webp");
        Long docWebp = insertMedia("doc-webp", "DOCUMENT", "application/octet-stream", "webp");

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"resumeMediaId\":" + image + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"resumeMediaId\":" + docWebp + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(withCsrf(put("/api/v1/admin/about")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"resumeMediaId\":" + pdf + "}"), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeMediaId").value(pdf));
    }

    // ---------------------------------------------------------------
    // selected content
    // ---------------------------------------------------------------

    @Test
    void selectedLimitEnforcedPerType() throws Exception {
        MockHttpSession session = loginSession();
        Long t1 = insertTutorial("t1", "PUBLISHED");
        Long t2 = insertTutorial("t2", "PUBLISHED");
        Long t3 = insertTutorial("t3", "PUBLISHED");
        Long t4 = insertTutorial("t4", "PUBLISHED");
        mockMvc.perform(withCsrf(put("/api/v1/admin/about/selected-content")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"tutorialIds\":[" + t1 + "," + t2 + "," + t3 + "," + t4 + "]}"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SELECTED_LIMIT_EXCEEDED"));
    }

    @Test
    void selectedUnknownContentRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(put("/api/v1/admin/about/selected-content")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"tutorialIds\":[999999]}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"));
    }

    @Test
    void selectedDedupeOrderAndDraftAllowed() throws Exception {
        Long t1 = insertTutorial("t1", "PUBLISHED");
        Long t2 = insertTutorial("t2", "PUBLISHED");
        Long draftTutorial = insertTutorial("t-draft", "DRAFT");

        selectContent("[" + t2 + "," + t1 + "," + t2 + "," + draftTutorial + "]", "[]", "[]");

        MockHttpSession session = loginSession();
        // input order preserved, duplicates removed; draft selected in admin
        mockMvc.perform(get("/api/v1/admin/about").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTutorialIds[0]").value(t2))
                .andExpect(jsonPath("$.selectedTutorialIds[1]").value(t1))
                .andExpect(jsonPath("$.selectedTutorialIds[2]").value(draftTutorial))
                .andExpect(jsonPath("$.selectedTutorialIds.length()").value(3));

        // public only shows published, in display order
        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTutorials.length()").value(2))
                .andExpect(jsonPath("$.selectedTutorials[0].slug").value("t2"))
                .andExpect(jsonPath("$.selectedTutorials[1].slug").value("t1"));
    }

    @Test
    void withdrawKeepsRelationAndRepublishRestores() throws Exception {
        Long blogId = insertBlog("b1", "PUBLISHED");
        selectContent("[]", "[" + blogId + "]", "[]");

        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBlogs.length()").value(1));

        // withdraw the post -> public hides it, admin relation stays
        jdbc.update("UPDATE blog_post SET publish_status = 'WITHDRAWN' WHERE id = ?", blogId);
        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBlogs").isEmpty());
        MockHttpSession session = loginSession();
        mockMvc.perform(get("/api/v1/admin/about").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBlogPostIds[0]").value(blogId));

        // republish -> public restores automatically
        jdbc.update("UPDATE blog_post SET publish_status = 'PUBLISHED' WHERE id = ?", blogId);
        mockMvc.perform(get("/api/v1/public/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBlogs.length()").value(1))
                .andExpect(jsonPath("$.selectedBlogs[0].slug").value("b1"));
    }

    @Test
    void deleteContentCascadesRelationAway() throws Exception {
        Long portfolioId = insertPortfolio("p1", "PUBLISHED");
        selectContent("[]", "[]", "[" + portfolioId + "]");

        // delete the project -> relation removed via FK cascade
        jdbc.update("DELETE FROM portfolio_project WHERE id = ?", portfolioId);

        MockHttpSession session = loginSession();
        mockMvc.perform(get("/api/v1/admin/about").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedPortfolioProjectIds").isEmpty());
    }

    @Test
    void unauthenticatedAboutAdminRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/about"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

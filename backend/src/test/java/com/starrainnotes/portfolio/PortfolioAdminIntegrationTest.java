package com.starrainnotes.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-007 — admin portfolio projects: independent publish/project statuses,
 * ONLINE requires demoUrl, featured limit, date range, tech stack
 * normalization and IMAGE-only covers.
 */
class PortfolioAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
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

    private Long createProject(MockHttpSession session, String slug, String extraJson) throws Exception {
        String body = "{\"title\":\"Project " + slug + "\",\"slug\":\"" + slug + "\","
                + "\"summary\":\"Summary\",\"bodyMarkdown\":\"Body\""
                + (extraJson == null || extraJson.isEmpty() ? "" : "," + extraJson) + "}";
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects", body),
                        csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String statusOf(Long projectId, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM portfolio_project WHERE id = ?",
                String.class, projectId);
    }

    @Test
    void createStartsWithDraftAndGivenProjectStatus() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-dev\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"projectStatus\":\"DEVELOPING\"}"), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.projectStatus").value("DEVELOPING"))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void publishedWhileDevelopingIsLegal() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "p-dev", "\"projectStatus\":\"DEVELOPING\"");
        mockMvc.perform(withCsrf(post("/api/v1/admin/portfolio/projects/" + projectId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.projectStatus").value("DEVELOPING"));
    }

    @Test
    void onlineRequiresDemoUrl() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-online\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"projectStatus\":\"ONLINE\"}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DEMO_URL_REQUIRED"));

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-online-ok\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"projectStatus\":\"ONLINE\",\"demoUrl\":\"https://demo.example.com\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectStatus").value("ONLINE"));
    }

    @Test
    void invalidProjectStatusRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-bad\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"projectStatus\":\"BOGUS\"}"), csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void featuredLimitEnforced() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createProject(session, "f1", "\"featured\":true");
        Long b = createProject(session, "f2", "\"featured\":true");
        Long c = createProject(session, "f3", "\"featured\":true");

        // 4th featured rejected
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"f4\",\"summary\":\"S\",\"bodyMarkdown\":\"B\",\"featured\":true}"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FEATURED_LIMIT_EXCEEDED"));

        // un-feature one, then the 4th is allowed
        mockMvc.perform(withCsrf(put("/api/v1/admin/portfolio/projects/" + a)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"slug\":\"f1\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"featured\":false}"), csrf(session)).session(session))
                .andExpect(status().isOk());

        Long d = createProject(session, "f4", "\"featured\":true");
        assertThat(d).isPositive();

        // updating an already-featured project keeps counting itself out
        mockMvc.perform(withCsrf(put("/api/v1/admin/portfolio/projects/" + d)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"D\",\"slug\":\"f4\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"featured\":true}"), csrf(session)).session(session))
                .andExpect(status().isOk());
        assertThat(statusOf(b, "featured")).isEqualTo("1");
    }

    @Test
    void completedBeforeStartedRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-dates\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"startedAt\":\"2026-08-10\",\"completedAt\":\"2026-08-01\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        // equal dates are fine
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-dates-ok\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"startedAt\":\"2026-08-01\",\"completedAt\":\"2026-08-01\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isCreated());
    }

    @Test
    void techStackNormalizedAndLimited() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "p-stack",
                "\"techStack\":[\" Java \",\"\",\"java\",\" Spring \"]");
        mockMvc.perform(get("/api/v1/admin/portfolio/projects/" + projectId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.techStack[0]").value("Java"))
                .andExpect(jsonPath("$.techStack[1]").value("java"))
                .andExpect(jsonPath("$.techStack[2]").value("Spring"))
                .andExpect(jsonPath("$.techStack.length()").value(3));

        // 21 items rejected
        String twentyOne = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(i -> "\"item-" + i + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-stack-big\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"techStack\":" + twentyOne + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TECH_STACK_TOO_LARGE"));
    }

    @Test
    void coverMustBeImage() throws Exception {
        MockHttpSession session = loginSession();
        Long documentId = insertMedia("doc", "DOCUMENT");
        Long imageId = insertMedia("img", "IMAGE");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-cover-bad\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"coverMediaId\":" + documentId + "}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_INVALID"));

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"P\",\"slug\":\"p-cover-ok\",\"summary\":\"S\",\"bodyMarkdown\":\"B\","
                                + "\"coverMediaId\":" + imageId + "}"), csrf(session)).session(session))
                .andExpect(status().isCreated());
    }

    @Test
    void publishWithdrawKeepsPublishedAt() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "p-lifecycle", null);

        mockMvc.perform(withCsrf(post("/api/v1/admin/portfolio/projects/" + projectId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PUBLISH_TRANSITION"));

        mockMvc.perform(withCsrf(post("/api/v1/admin/portfolio/projects/" + projectId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
        java.sql.Timestamp first = jdbc.queryForObject(
                "SELECT published_at FROM portfolio_project WHERE id = ?", java.sql.Timestamp.class, projectId);

        mockMvc.perform(withCsrf(post("/api/v1/admin/portfolio/projects/" + projectId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("WITHDRAWN"));
        java.sql.Timestamp afterWithdraw = jdbc.queryForObject(
                "SELECT published_at FROM portfolio_project WHERE id = ?", java.sql.Timestamp.class, projectId);
        assertThat(afterWithdraw).isEqualTo(first);
    }

    @Test
    void updateCannotCarryPublishStatus() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "p-no-status", null);

        mockMvc.perform(withCsrf(put("/api/v1/admin/portfolio/projects/" + projectId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"slug\":\"p-no-status\",\"summary\":\"S\","
                                + "\"bodyMarkdown\":\"B\",\"publishStatus\":\"PUBLISHED\"}"), csrf(session))
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        assertThat(statusOf(projectId, "publish_status")).isEqualTo("DRAFT");
    }

    @Test
    void listPaginatesAndDeleteSucceeds() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createProject(session, "p1", "\"projectStatus\":\"DEVELOPING\",\"role\":\"Backend\",\"techStack\":[\"Java\"]");
        createProject(session, "p2", "\"projectStatus\":\"COMPLETED\"");

        mockMvc.perform(get("/api/v1/admin/portfolio/projects").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].summary").value("Summary"))
                .andExpect(jsonPath("$.items[0].techStack").isArray());

        mockMvc.perform(get("/api/v1/admin/portfolio/projects")
                        .param("projectStatus", "DEVELOPING").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("p1"))
                .andExpect(jsonPath("$.items[0].role").value("Backend"))
                .andExpect(jsonPath("$.items[0].techStack[0]").value("Java"));

        mockMvc.perform(withCsrf(delete("/api/v1/admin/portfolio/projects/" + a), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/portfolio/projects/" + a).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void unauthenticatedPortfolioAdminRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/portfolio/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

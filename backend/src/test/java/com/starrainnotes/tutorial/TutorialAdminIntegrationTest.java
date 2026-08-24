package com.starrainnotes.tutorial;

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
 * TASK-005A — admin tutorial CRUD + publish lifecycle:
 * publishStatus is never changeable via plain PUT; first publish stamps
 * publishedAt; withdraw/republish keep it.
 */
class TutorialAdminIntegrationTest extends AbstractAuthIntegrationTest {

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
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
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

    private Long createCategory(String slug) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, slug);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long createTutorial(MockHttpSession session, Long categoryId, String slug) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials",
                        tutorialJson(categoryId, slug)), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String tutorialJson(Long categoryId, String slug) {
        return "{\"categoryId\":" + categoryId + ",\"title\":\"Title " + slug + "\",\"slug\":\"" + slug
                + "\",\"summary\":\"Summary " + slug + "\",\"sortOrder\":0}";
    }

    private String publishStatusOf(Long tutorialId) {
        return jdbc.queryForObject(
                "SELECT publish_status FROM tutorial WHERE id = ?", String.class, tutorialId);
    }

    @Test
    void createTutorialStartsAsDraft() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials",
                        tutorialJson(categoryId, "t-create")), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createTutorialDuplicateSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        createTutorial(session, categoryId, "dup");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials",
                        tutorialJson(categoryId, "dup")), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
    }

    @Test
    void createTutorialMissingCategoryRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials",
                        tutorialJson(999999L, "t-nocat")), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TUTORIAL_CATEGORY_NOT_FOUND"));
    }

    @Test
    void createTutorialInvalidSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials",
                        "{\"categoryId\":" + categoryId + ",\"title\":\"T\",\"slug\":\"Bad Slug\",\"summary\":\"S\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateTutorialChangesFieldsButNotStatus() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-upd");

        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorials/" + tutorialId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + categoryId + ",\"title\":\"New Title\",\"slug\":\"t-upd\","
                                + "\"summary\":\"New Summary\",\"sortOrder\":5}"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"));
    }

    @Test
    void updateTutorialCannotCarryPublishStatusField() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-no-status");

        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorials/" + tutorialId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + categoryId + ",\"title\":\"T\",\"slug\":\"t-no-status\","
                                + "\"summary\":\"S\",\"publishStatus\":\"PUBLISHED\"}"), csrf(session))
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        assertThat(publishStatusOf(tutorialId)).isEqualTo("DRAFT");
    }

    @Test
    void publishSetsPublishedAtOnFirstPublish() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-pub");

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")));
    }

    @Test
    void withdrawAndRepublishKeepFirstPublishedAt() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-cycle");

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        java.sql.Timestamp firstPublishedAt = jdbc.queryForObject(
                "SELECT published_at FROM tutorial WHERE id = ?", java.sql.Timestamp.class, tutorialId);
        assertThat(firstPublishedAt).isNotNull();

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("WITHDRAWN"));
        assertThat(publishedAtOf(tutorialId)).isEqualTo(firstPublishedAt);

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
        assertThat(publishedAtOf(tutorialId)).isEqualTo(firstPublishedAt);
    }

    private java.sql.Timestamp publishedAtOf(Long tutorialId) {
        return jdbc.queryForObject(
                "SELECT published_at FROM tutorial WHERE id = ?", java.sql.Timestamp.class, tutorialId);
    }

    @Test
    void withdrawDraftRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-draft-withdraw");

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PUBLISH_TRANSITION"));
    }

    @Test
    void listReturnsPagedTutorialsWithStatusFilter() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long t1 = createTutorial(session, categoryId, "t-list-1");
        createTutorial(session, categoryId, "t-list-2");
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + t1 + "/publish"), csrf(session)).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/tutorials").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/admin/tutorials").param("status", "PUBLISHED").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Title t-list-1"));

        mockMvc.perform(get("/api/v1/admin/tutorials").param("q", "t-list-2").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("t-list-2"));
    }

    @Test
    void moveTutorialReordersWithinItsCategory() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat-move");
        Long first = createTutorial(session, categoryId, "t-first");
        Long second = createTutorial(session, categoryId, "t-second");
        Long third = createTutorial(session, categoryId, "t-third");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + third + "/move",
                        "{\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/tutorials")
                        .param("categoryId", String.valueOf(categoryId)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(third))
                .andExpect(jsonPath("$.items[1].id").value(first))
                .andExpect(jsonPath("$.items[2].id").value(second));
        assertThat(jdbc.queryForObject("SELECT sort_order FROM tutorial WHERE id = ?", Integer.class, third))
                .isEqualTo(10);
    }

    @Test
    void detailReturnsFullView() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-detail");

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tutorialId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("cat"))
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"));
    }

    @Test
    void deleteTutorialWithoutNodesSucceeds() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-del");

        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorials/" + tutorialId), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_FOUND"));
    }

    @Test
    void deleteTutorialWithNodesRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long categoryId = createCategory("cat");
        Long tutorialId = createTutorial(session, categoryId, "t-del-nodes");
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title)
                VALUES (?, 'GROUP', 'g1')
                """, tutorialId);

        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorials/" + tutorialId), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TUTORIAL_HAS_NODES"));
    }

    @Test
    void unauthenticatedTutorialAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tutorials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

package com.starrainnotes.tutorial;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-005A — admin tutorial category tree / create / update / delete with
 * cycle protection and delete guards.
 */
class TutorialCategoryAdminIntegrationTest extends AbstractAuthIntegrationTest {

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

    private Long createCategory(MockHttpSession session, String name, String slug, Long parentId) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        "{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\","
                                + (parentId == null ? "" : "\"parentId\":" + parentId + ",")
                                + "\"sortOrder\":0}"), csrf(session))
                        .session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String categoryJson(String name, String slug, Long parentId) {
        return "{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\","
                + (parentId == null ? "" : "\"parentId\":" + parentId + ",") + "\"sortOrder\":0}";
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPut(String url, String body) {
        return put(url).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void createCategorySucceedsAndAppearsInTree() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        categoryJson("Java", "java", null)), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.slug").value("java"));

        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    void createCategoryDuplicateSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        createCategory(session, "Java", "java", null);
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        categoryJson("Java2", "java", null)), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
    }

    @Test
    void createCategoryWithMissingParentRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        "{\"name\":\"X\",\"slug\":\"x\",\"parentId\":999999}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_CATEGORY_NOT_FOUND"));
    }

    @Test
    void createCategoryInvalidSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        "{\"name\":\"Bad\",\"slug\":\"Bad Slug!\"}"), csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateCategoryMovesUnderParent() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "Backend", "backend", null);
        Long b = createCategory(session, "Java", "java", null);

        mockMvc.perform(withCsrf(jsonPut("/api/v1/admin/tutorial-categories/" + b,
                        categoryJson("Java", "java", a)), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(a));

        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].children[0].slug").value("java"));
    }

    @Test
    void updateCategoryCycleRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        Long b = createCategory(session, "B", "b", a);

        // move A under its own descendant B -> cycle
        mockMvc.perform(withCsrf(jsonPut("/api/v1/admin/tutorial-categories/" + a,
                        categoryJson("A", "a", b)), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_CYCLE"));
    }

    @Test
    void updateCategorySelfParentRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        mockMvc.perform(withCsrf(jsonPut("/api/v1/admin/tutorial-categories/" + a,
                        categoryJson("A", "a", a)), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_CYCLE"));
    }

    @Test
    void updateCategoryMissingTargetRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPut("/api/v1/admin/tutorial-categories/999999",
                        categoryJson("A", "a", null)), csrf(session)).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void moveCategoryReordersRootSiblings() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        Long b = createCategory(session, "B", "b", null);
        Long c = createCategory(session, "C", "c", null);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories/" + c + "/move",
                        "{\"targetParentId\":null,\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(c))
                .andExpect(jsonPath("$[1].id").value(a))
                .andExpect(jsonPath("$[2].id").value(b));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT sort_order FROM tutorial_category WHERE id = ?", Integer.class, c)).isEqualTo(10);
    }

    @Test
    void deleteCategoryWithChildrenRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        createCategory(session, "B", "b", a);
        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorial-categories/" + a), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_CHILDREN"));
    }

    @Test
    void deleteCategoryWithTutorialsRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status)
                VALUES (?, 't', 't', 'summary', 'DRAFT')
                """, a);
        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorial-categories/" + a), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_TUTORIALS"));
    }

    @Test
    void deleteLeafCategorySucceeds() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a", null);
        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorial-categories/" + a), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unauthenticatedCategoryAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flat knowledge-system CRUD and sibling ordering. */
class TutorialCategoryAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("DELETE FROM tutorial_category");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("DELETE FROM tutorial_category");
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"),
                        fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private Long createCategory(MockHttpSession session, String name, String slug) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        categoryJson(name, slug)), csrf(session)).session(session))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String categoryJson(String name, String slug) {
        return "{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\",\"sortOrder\":0}";
    }

    @Test
    void createAndUpdateCategoryKeepItAtRoot() throws Exception {
        MockHttpSession session = loginSession();
        Long id = createCategory(session, "Java 全栈", "java-fullstack");

        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorial-categories/" + id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(categoryJson("Java 工程体系", "java-engineering")), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java 工程体系"))
                .andExpect(jsonPath("$.parentId").isEmpty())
                .andExpect(jsonPath("$.children").isEmpty());
    }

    @Test
    void duplicateAndInvalidSlugsAreRejected() throws Exception {
        MockHttpSession session = loginSession();
        createCategory(session, "Java", "java");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        categoryJson("Java 2", "java")), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories",
                        categoryJson("Bad", "Bad Slug!")), csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void moveCategoryReordersOnlyRootSiblings() throws Exception {
        MockHttpSession session = loginSession();
        Long a = createCategory(session, "A", "a");
        Long b = createCategory(session, "B", "b");
        Long c = createCategory(session, "C", "c");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorial-categories/" + c + "/move",
                        "{\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(c))
                .andExpect(jsonPath("$[1].id").value(a))
                .andExpect(jsonPath("$[2].id").value(b))
                .andExpect(jsonPath("$[0].children").isEmpty());
    }

    @Test
    void deleteGuardAndEmptyDeleteWork() throws Exception {
        MockHttpSession session = loginSession();
        Long occupied = createCategory(session, "A", "a");
        jdbc.update("INSERT INTO tutorial (category_id, title, slug, summary, publish_status) "
                + "VALUES (?, 't', 't', 'summary', 'DRAFT')", occupied);

        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorial-categories/" + occupied), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_TUTORIALS"));

        Long empty = createCategory(session, "B", "b");
        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorial-categories/" + empty), csrf(session)).session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingAndUnauthenticatedAccessAreRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorial-categories/999999")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(categoryJson("A", "a")), csrf(session)).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/admin/tutorial-categories/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

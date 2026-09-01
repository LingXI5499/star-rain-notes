package com.starrainnotes.english;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-008 — English overview: V1 currentStage stays FOUNDATION and the admin
 * request cannot change it.
 */
class EnglishIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndReset() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        resetSingleton();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        resetSingleton();
    }

    private void resetSingleton() {
        jdbc.update("""
                UPDATE english_overview
                SET title = 'English', subtitle = 'Build English as a long-term skill.',
                    introduction = NULL, current_stage = 'FOUNDATION', roadmap_markdown = ''
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

    @Test
    void publicEnglishReturnsSeedWithFoundationStage() throws Exception {
        mockMvc.perform(get("/api/v1/public/english"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("English"))
                .andExpect(jsonPath("$.subtitle").value("Build English as a long-term skill."))
                .andExpect(jsonPath("$.currentStage").value("FOUNDATION"));
    }

    @Test
    void adminCanUpdateButNotCurrentStage() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = csrf(session);

        // PUT carrying currentStage is rejected (fail-on-unknown)
        mockMvc.perform(withCsrf(put("/api/v1/admin/english")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"English\",\"subtitle\":\"S\",\"roadmapMarkdown\":\"R\","
                                + "\"currentStage\":\"READING\"}"), csrf).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        // legitimate update keeps FOUNDATION
        mockMvc.perform(withCsrf(put("/api/v1/admin/english")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"English 学习\",\"subtitle\":\"新副标题\","
                                + "\"introduction\":\"介绍\",\"roadmapMarkdown\":\"## Roadmap\"}"), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("English 学习"))
                .andExpect(jsonPath("$.currentStage").value("FOUNDATION"));

        String stage = jdbc.queryForObject("SELECT current_stage FROM english_overview WHERE id = 1", String.class);
        assertThat(stage).isEqualTo("FOUNDATION");
    }

    @Test
    void unauthenticatedEnglishAdminRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

package com.starrainnotes.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-003 — setup vertical slice against a real MySQL database:
 * fresh system requires setup; wrong token rejected (constant-time);
 * correct setup succeeds; second setup is impossible; CSRF is required.
 */
@SpringBootTest(properties = "app.setup-token=setup-test-token-abc")
class SetupIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanAdminTable() {
        jdbc.update("DELETE FROM admin_user");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
    }

    private long adminCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user", Long.class);
        return count == null ? 0 : count;
    }

    @Test
    void freshSystemRequiresSetup() throws Exception {
        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void wrongSetupTokenRejected() throws Exception {
        String csrf = fetchCsrfToken();
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}"), csrf)
                        .header("X-Setup-Token", "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("SETUP_TOKEN_MISMATCH"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();
        // no stack trace / SQL internals leaked
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Exception", "Caused by", "passwordHash", "$2");
        assertThat(adminCount()).isZero();
    }

    @Test
    void missingSetupTokenHeaderRejected() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}"), csrf))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SETUP_TOKEN_MISMATCH"));
        assertThat(adminCount()).isZero();
    }

    @Test
    void setupRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/setup/admin")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}")
                        .header("X-Setup-Token", "setup-test-token-abc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        assertThat(adminCount()).isZero();
    }

    @Test
    void correctSetupSucceeds() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}"), csrf)
                        .header("X-Setup-Token", "setup-test-token-abc"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"));

        assertThat(adminCount()).isEqualTo(1);
        String passwordHash = jdbc.queryForObject(
                "SELECT password_hash FROM admin_user WHERE username = 'admin'", String.class);
        assertThat(passwordHash).isNotEqualTo("admin-pass-1234").startsWith("$2");
    }

    @Test
    void setupValidatesBody() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"\",\"password\":\"short\"}"), csrf)
                        .header("X-Setup-Token", "setup-test-token-abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations").isArray());
        assertThat(adminCount()).isZero();
    }

    @Test
    void secondSetupImpossible() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}"), csrf)
                        .header("X-Setup-Token", "setup-test-token-abc"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));

        String csrf2 = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin2\",\"password\":\"admin-pass-5678\"}"), csrf2)
                        .header("X-Setup-Token", "setup-test-token-abc"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SETUP_ALREADY_COMPLETED"));
        assertThat(adminCount()).isEqualTo(1);
    }
}

package com.starrainnotes.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-003 — when APP_SETUP_TOKEN is not configured, setup cannot proceed
 * (503 SETUP_TOKEN_NOT_CONFIGURED) and no admin is created.
 */
@SpringBootTest
class SetupTokenNotConfiguredIntegrationTest extends AbstractAuthIntegrationTest {

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

    @Test
    void setupUnavailableWithoutConfiguredToken() throws Exception {
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/setup/admin",
                        "{\"username\":\"admin\",\"password\":\"admin-pass-1234\"}"), csrf)
                        .header("X-Setup-Token", "anything"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SETUP_TOKEN_NOT_CONFIGURED"));

        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user", Long.class);
        assertThat(count).isZero();
    }
}

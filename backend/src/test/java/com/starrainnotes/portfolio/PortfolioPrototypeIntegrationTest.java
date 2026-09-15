package com.starrainnotes.portfolio;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZIP / static prototype upload is paused; online access uses live URL or repository.
 */
class PortfolioPrototypeIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdmin() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void uploadPrototypeIsDisabled() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = fetchCsrfToken();
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.zip", "application/zip", new byte[]{0x50, 0x4b, 0x03, 0x04});
        mockMvc.perform(withCsrf(multipart("/api/v1/admin/portfolio/projects/1/prototype").file(file), csrf)
                        .session(session))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("PROTOTYPE_UPLOAD_DISABLED"));
    }

    private MockHttpSession loginSession() throws Exception {
        String csrf = fetchCsrfToken();
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), csrf))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

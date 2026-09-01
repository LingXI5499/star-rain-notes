package com.starrainnotes.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the production-only legacy authentication shutdown switches. */
@SpringBootTest(properties = {
        "app.legacy-admin-login-enabled=false",
        "app.legacy-setup-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegacyAuthDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacySetupStatusIsGone() throws Exception {
        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("LEGACY_SETUP_DISABLED"));
    }

    @Test
    void legacyUsernameLoginIsGone() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"legacy\",\"password\":\"irrelevant-password\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("LEGACY_LOGIN_DISABLED"));
    }
}

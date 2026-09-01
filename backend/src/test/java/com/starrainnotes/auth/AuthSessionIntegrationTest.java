package com.starrainnotes.auth;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-003 — session authentication against a real MySQL database:
 * login persists the SecurityContext into the HttpSession; logout and password
 * change invalidate it; 401/403/CSRF failures are RFC 9457 Problem Details;
 * the CSRF token is rotated after login/logout/password change.
 */
class AuthSessionIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private static final String NEW_PASSWORD = "new-pass-5678";

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

    private MvcResult login(String password, MockHttpSession session) throws Exception {
        return mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + password + "\"}"), fetchCsrfToken())
                        .session(session == null ? new MockHttpSession() : session))
                .andReturn();
    }

    private MockHttpSession loginAndGetSession() throws Exception {
        MvcResult result = login(PASSWORD, null);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ---------------------------------------------------------------
    // login / session persistence
    // ---------------------------------------------------------------

    @Test
    void incorrectPasswordReturns401Problem() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"wrong-password\"}"), fetchCsrfToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Exception", "Caused by", "passwordHash", "$2");
    }

    @Test
    void unknownUserReturns401Problem() throws Exception {
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"nobody\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void correctLoginCreatesPersistentSession() throws Exception {
        MvcResult result = login(PASSWORD, null);
        mockMvc.perform(get("/api/v1/auth/session").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("ROLE_SUPER_ADMIN"));

        // authenticated request passes the security filter chain (200, not 401)
        mockMvc.perform(get("/api/v1/admin/dashboard").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousSessionViewIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void loginValidatesRequestBody() throws Exception {
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"\",\"password\":\"\"}"), fetchCsrfToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ---------------------------------------------------------------
    // authorization / CSRF problem details
    // ---------------------------------------------------------------

    @Test
    void anonymousAdminApiReturns401ProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Exception", "Caused by", "passwordHash");
    }

    @Test
    void invalidCsrfReturns403ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void anonymousLogoutRejected() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/logout"), fetchCsrfToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void anonymousChangePasswordRejected() throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/auth/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"y\"}"), fetchCsrfToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------
    // CSRF rotation after login / logout
    // ---------------------------------------------------------------

    @Test
    void csrfTokenRefreshesAfterLogin() throws Exception {
        String tokenBefore = fetchCsrfToken();
        MockHttpSession session = loginAndGetSession();

        String tokenAfter = fetchCsrfTokenFor(session);
        assertThat(tokenAfter).isNotEqualTo(tokenBefore).isNotBlank();
    }

    @Test
    void csrfTokenRefreshesAfterLogout() throws Exception {
        MockHttpSession session = loginAndGetSession();
        String tokenBeforeLogout = fetchCsrfTokenFor(session);

        mockMvc.perform(withCsrf(post("/api/v1/auth/logout"), tokenBeforeLogout).session(session))
                .andExpect(status().isNoContent());

        String tokenAfterLogout = fetchCsrfTokenFor(session);
        assertThat(tokenAfterLogout).isNotEqualTo(tokenBeforeLogout).isNotBlank();
    }

    // ---------------------------------------------------------------
    // logout / password change invalidate the session
    // ---------------------------------------------------------------

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = loginAndGetSession();
        String csrf = fetchCsrfTokenFor(session);

        mockMvc.perform(withCsrf(post("/api/v1/auth/logout"), csrf).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));

        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void passwordChangeInvalidatesSessionAndRequiresRelogin() throws Exception {
        MockHttpSession session = loginAndGetSession();
        String csrf = fetchCsrfTokenFor(session);

        mockMvc.perform(withCsrf(put("/api/v1/auth/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD
                                + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"), csrf)
                        .session(session))
                .andExpect(status().isNoContent());

        // same session is no longer authenticated
        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));

        // old password rejected, new password works
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isUnauthorized());

        MvcResult relogin = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + NEW_PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();
        assertThat(relogin.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void passwordChangeWithWrongCurrentPasswordRejected() throws Exception {
        MockHttpSession session = loginAndGetSession();
        String csrf = fetchCsrfTokenFor(session);

        mockMvc.perform(withCsrf(put("/api/v1/auth/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-current\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"), csrf)
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        // session still valid
        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    private String fetchCsrfTokenFor(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }
}

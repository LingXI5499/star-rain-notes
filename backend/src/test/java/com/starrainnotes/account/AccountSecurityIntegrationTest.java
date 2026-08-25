package com.starrainnotes.account;

import com.starrainnotes.account.service.MailGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.account.super-admin-email=super@example.com",
        "app.account.mail.base-url=http://localhost"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean MailGateway mailGateway;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM email_verification_challenge");
        jdbc.update("DELETE FROM admin_invitation");
        jdbc.update("DELETE FROM user_account");
    }
    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM email_verification_challenge");
        jdbc.update("DELETE FROM admin_invitation");
        jdbc.update("DELETE FROM user_account");
    }

    private String csrf() throws Exception {
        return mockMvc.perform(get("/api/v1/auth/csrf")).andReturn()
                .getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private void doPost(String url, String body, String token) throws Exception {
        mockMvc.perform(post(url).contentType("application/json").content(body)
                .header("X-XSRF-TOKEN", token)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)));
    }

    @Test
    void activationFlowAndAlreadyActivated() throws Exception {
        String token = csrf();
        mockMvc.perform(get("/api/v1/auth/super-admin-activation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.activated").value(false))
                .andExpect(jsonPath("$.emailMasked").value("su***@example.com"));

        doPost("/api/v1/auth/super-admin-activation/verification-codes", "{}", token);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mailGateway, atLeastOnce()).sendVerificationCode(anyString(), anyString(), captor.capture());
        String code = captor.getValue();
        assertThat(code).matches("\\d{6}");

        // wrong code consumed an attempt; correct code still works
        doPost("/api/v1/auth/super-admin-activation/confirm",
                "{\"verificationCode\":\"000000\",\"password\":\"pass1234567\"}", token);
        doPost("/api/v1/auth/super-admin-activation/confirm",
                "{\"verificationCode\":\"" + code + "\",\"password\":\"pass1234567\"}", token);

        mockMvc.perform(get("/api/v1/auth/super-admin-activation/status"))
                .andExpect(jsonPath("$.activated").value(true));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_account WHERE role='SUPER_ADMIN' AND account_status='ACTIVE'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void secondSuperAdminCannotBeActivated() throws Exception {
        String token = csrf();
        doPost("/api/v1/auth/super-admin-activation/verification-codes", "{}", token);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mailGateway, atLeastOnce()).sendVerificationCode(anyString(), anyString(), captor.capture());
        doPost("/api/v1/auth/super-admin-activation/confirm",
                "{\"verificationCode\":\"" + captor.getValue() + "\",\"password\":\"pass1234567\"}", token);

        mockMvc.perform(post("/api/v1/auth/super-admin-activation/verification-codes")
                        .header("X-XSRF-TOKEN", token)
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SUPER_ADMIN_ALREADY_ACTIVATED"));
    }

    @Test
    void loginIsLockedAfterFiveFailures() throws Exception {
        jdbc.update("INSERT INTO user_account(email,password_hash,role,account_status,email_verified_at,activated_at,auth_version)"
                + " VALUES ('admin@example.com',?,'ADMIN','ACTIVE',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),1)",
                passwordEncoder.encode("realpass123"));
        String token = csrf();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/account/login")
                            .contentType("application/json")
                            .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}")
                            .header("X-XSRF-TOKEN", token).cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/account/login")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}")
                        .header("X-XSRF-TOKEN", token).cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_TEMPORARILY_LOCKED"));
    }
}
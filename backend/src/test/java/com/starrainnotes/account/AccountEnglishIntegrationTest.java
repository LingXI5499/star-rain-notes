package com.starrainnotes.account;

import com.starrainnotes.account.service.MailGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.account.super-admin-email=super@example.com",
        "app.account.mail.base-url=http://localhost"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountEnglishIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean MailGateway mailGateway;

    private Long vocabThemeId;
    private Long vocabWordId;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM english_learning_attempt");
        jdbc.update("DELETE FROM english_learning_record");
        jdbc.update("DELETE FROM english_writing_submission");
        jdbc.update("DELETE FROM account_vocabulary_memory");
        jdbc.update("DELETE FROM english_learner_profile");
        jdbc.update("DELETE FROM content_review_request");
        jdbc.update("DELETE FROM admin_audit_log");
        jdbc.update("DELETE FROM email_verification_challenge");
        jdbc.update("DELETE FROM admin_invitation");
        jdbc.update("DELETE FROM user_account");
        jdbc.update("DELETE FROM english_writing_prompt WHERE slug='account-test-prompt'");
    }

    @AfterEach
    void cleanUp() {
        if (vocabWordId != null) jdbc.update("DELETE FROM vocabulary_word WHERE id=?", vocabWordId);
        if (vocabThemeId != null) jdbc.update("DELETE FROM vocabulary_theme WHERE id=?", vocabThemeId);
        clean();
    }

    private void insertAccountVocabularyThemeAndWord() {
        jdbc.update("INSERT INTO vocabulary_theme(layer,layer_order,name,sort_order) VALUES ('A',1,'account-test-theme',1)");
        vocabThemeId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO vocabulary_word(theme_id,part_of_speech,word,phonetic_us,translation,inflections,examples,sort_order)
                VALUES (?, '名词 n.', 'accounttestword', '/tɛst/', '测试词', NULL, '[]', 1)
                """, vocabThemeId);
        vocabWordId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String csrf() throws Exception {
        return mockMvc.perform(get("/api/v1/auth/csrf")).andReturn()
                .getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private void insertAccount(String email, String password, String role) {
        jdbc.update("INSERT INTO user_account(email,password_hash,role,account_status,email_verified_at,activated_at,auth_version)"
                        + " VALUES (?,?,?,'ACTIVE',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),1)",
                email, passwordEncoder.encode(password), role);
    }

    private MockHttpSession loginAccount(String email, String password) throws Exception {
        String token = csrf();
        return (MockHttpSession) mockMvc.perform(post("/api/v1/auth/account/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                        .header("X-XSRF-TOKEN", token)
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private void doPut(String url, String body, MockHttpSession session, String token) throws Exception {
        mockMvc.perform(put(url).session(session).contentType("application/json").content(body)
                        .header("X-XSRF-TOKEN", token)
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void recordsSummaryInsightsAndBatch() throws Exception {
        insertAccount("learn@example.com", "learn-pass-1234", "ADMIN");
        MockHttpSession session = loginAccount("learn@example.com", "learn-pass-1234");
        String token = csrf();

        doPut("/api/v1/account/english/learning/records/GRAMMAR/1",
                "{\"completionStatus\":\"COMPLETED\",\"timeSpentSeconds\":120}", session, token);
        doPut("/api/v1/account/english/learning/records/READING/2",
                "{\"completionStatus\":\"IN_PROGRESS\",\"timeSpentSeconds\":60}", session, token);

        mockMvc.perform(get("/api/v1/account/english/learning/records/GRAMMAR/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completion_status").value("COMPLETED"))
                .andExpect(jsonPath("$.time_spent_seconds").value(120));

        mockMvc.perform(get("/api/v1/account/english/learning/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/api/v1/account/english/learning/insights").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(2))
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.activity").isArray());

        mockMvc.perform(get("/api/v1/account/english/learning/records/batch?ref=GRAMMAR:1&ref=READING:2").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['GRAMMAR:1'].status").value("COMPLETED"))
                .andExpect(jsonPath("$['READING:2'].status").value("IN_PROGRESS"));
    }

    @Test
    void vocabularyMemoryRoundTrip() throws Exception {
        insertAccount("learn@example.com", "learn-pass-1234", "ADMIN");
        MockHttpSession session = loginAccount("learn@example.com", "learn-pass-1234");
        String token = csrf();
        insertAccountVocabularyThemeAndWord();

        doPut("/api/v1/account/english/vocabulary/words/" + vocabWordId + "/memory",
                "{\"memoryCount\":3}", session, token);

        mockMvc.perform(get("/api/v1/account/english/vocabulary/memory").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].word_id").value(vocabWordId))
                .andExpect(jsonPath("$[0].memory_count").value(3));
    }

    @Test
    void writingSubmissionRoundTrip() throws Exception {
        jdbc.update("""
                INSERT INTO english_writing_prompt(title,slug,summary,background_markdown,requirements_markdown,
                    cefr_level,word_min,word_max,estimated_minutes,publish_status,published_at)
                VALUES ('Account test prompt','account-test-prompt','s','bg','req','B1',100,200,20,'PUBLISHED',UTC_TIMESTAMP(6))
                """);
        Long promptId = jdbc.queryForObject(
                "SELECT id FROM english_writing_prompt WHERE slug='account-test-prompt'", Long.class);

        insertAccount("learn@example.com", "learn-pass-1234", "ADMIN");
        MockHttpSession session = loginAccount("learn@example.com", "learn-pass-1234");
        String token = csrf();

        doPut("/api/v1/account/english/writing-submissions/" + promptId,
                "{\"bodyText\":\"Hello world from the account author.\",\"status\":\"DRAFT\",\"selfScore\":80}",
                session, token);

        MvcResult result = mockMvc.perform(get("/api/v1/account/english/writing-submissions/" + promptId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body_text").value("Hello world from the account author."))
                .andExpect(jsonPath("$.submission_status").value("DRAFT"))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString()).contains("Hello world");
    }

    @Test
    void accountEnglishEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/account/english/learning/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/account/english/vocabulary/memory"))
                .andExpect(status().isUnauthorized());
    }
}

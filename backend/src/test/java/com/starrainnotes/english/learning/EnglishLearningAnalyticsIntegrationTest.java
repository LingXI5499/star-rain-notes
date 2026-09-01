package com.starrainnotes.english.learning;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EnglishLearningAnalyticsIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String HASH_ONE = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_TWO = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    private long learnerOne;
    private long learnerTwo;
    private long readingId;

    @BeforeEach
    void seed() {
        cleanupRows();
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('analytics-admin',?)",
                passwordEncoder.encode("analytics-pass-123"));
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash) VALUES (?),(?)", HASH_ONE, HASH_TWO);
        learnerOne = jdbc.queryForObject("SELECT id FROM english_learner_profile WHERE learner_key_hash=?", Long.class, HASH_ONE);
        learnerTwo = jdbc.queryForObject("SELECT id FROM english_learner_profile WHERE learner_key_hash=?", Long.class, HASH_TWO);
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Analytics reading','analytics-reading','Summary','Body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
                """);
        readingId = jdbc.queryForObject("SELECT id FROM english_reading_article WHERE slug='analytics-reading'", Long.class);

        attempt(learnerOne, "READING", readingId, "analytics-reading", "COMPLETED", 90, 120,
                "[\"inference\",\"detail extraction\"]", ".8500", "UTC_TIMESTAMP(6)-INTERVAL 1 DAY");
        attempt(learnerTwo, "READING", readingId, "analytics-reading", "IN_PROGRESS", 70, 60,
                "[\"inference\"]", ".5000", "UTC_TIMESTAMP(6)-INTERVAL 2 DAY");
        attempt(learnerOne, "GRAMMAR", 1, "1-1", "COMPLETED", 80, 90,
                "[\"sentence structure\"]", ".7500", "UTC_TIMESTAMP(6)-INTERVAL 3 DAY");
        attempt(learnerOne, "READING", readingId, "analytics-reading", "COMPLETED", 60, 45,
                "[\"old weak point\"]", ".6000", "UTC_TIMESTAMP(6)-INTERVAL 40 DAY");
    }

    @AfterEach
    void cleanup() {
        cleanupRows();
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void requiresAuthenticationAndValidatesFilters() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english/analytics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        MockHttpSession session = login();
        mockMvc.perform(get("/api/v1/admin/english/analytics").session(session).param("days", "14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENGLISH_ANALYTICS_FILTER_INVALID"));
        mockMvc.perform(get("/api/v1/admin/english/analytics").session(session).param("type", "VOCABULARY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsAggregateTrendsAndModulesWithoutPrivateData() throws Exception {
        MockHttpSession session = login();
        MvcResult result = mockMvc.perform(get("/api/v1/admin/english/analytics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.contentType").value("ALL"))
                .andExpect(jsonPath("$.overview.activeLearners").value(2))
                .andExpect(jsonPath("$.overview.totalAttempts").value(3))
                .andExpect(jsonPath("$.overview.completions").value(2))
                .andExpect(jsonPath("$.overview.completionRate").value(66.7))
                .andExpect(jsonPath("$.overview.totalTimeSeconds").value(270))
                .andExpect(jsonPath("$.trend", hasSize(30)))
                .andExpect(jsonPath("$.modules", hasSize(4)))
                .andExpect(jsonPath("$.modules[?(@.contentType=='READING')].attempts").value(2))
                .andExpect(jsonPath("$.modules[?(@.contentType=='READING')].engagedContent").value(1))
                .andExpect(jsonPath("$.topContent").doesNotExist())
                .andExpect(jsonPath("$.weakPoints").doesNotExist())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("learnerId", "learnerKey", HASH_ONE, HASH_TWO, "bodyText", "submission");
    }

    @Test
    void filtersOneModuleAndExcludesAttemptsOutsideRange() throws Exception {
        MockHttpSession session = login();
        mockMvc.perform(get("/api/v1/admin/english/analytics").session(session)
                        .param("days", "7").param("type", "reading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("READING"))
                .andExpect(jsonPath("$.overview.totalAttempts").value(2))
                .andExpect(jsonPath("$.overview.completions").value(1))
                .andExpect(jsonPath("$.modules", hasSize(1)))
                .andExpect(jsonPath("$.modules[0].contentType").value("READING"))
                .andExpect(content().string(not(containsString("old weak point"))))
                .andExpect(content().string(not(containsString("sentence structure"))))
                .andExpect(content().string(not(containsString("averageMastery"))));
    }

    private void attempt(long learner, String type, long contentId, String slug, String status,
                         int score, int seconds, String weakPoints, String mastery, String at) {
        jdbc.update("INSERT INTO english_learning_attempt(learner_id,content_type,content_id,content_slug,cefr_level," +
                        "completion_status,score,time_spent_seconds,weak_points_json,mastery_level,attempted_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?," + mastery + "," + at + ")",
                learner, type, contentId, slug, "B1", status, score, seconds, weakPoints);
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"analytics-admin\",\"password\":\"analytics-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void cleanupRows() {
        jdbc.update("DELETE a FROM english_learning_attempt a JOIN english_learner_profile p ON p.id=a.learner_id WHERE p.learner_key_hash IN (?,?)", HASH_ONE, HASH_TWO);
        jdbc.update("DELETE FROM english_learner_profile WHERE learner_key_hash IN (?,?)", HASH_ONE, HASH_TWO);
        jdbc.update("DELETE FROM english_reading_article WHERE slug='analytics-reading'");
    }
}

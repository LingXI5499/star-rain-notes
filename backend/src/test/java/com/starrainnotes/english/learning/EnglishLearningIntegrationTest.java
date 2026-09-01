package com.starrainnotes.english.learning;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段四：游客进度改为本地存储，公开 X-Learner-Key 学习端点统一拦截为
 * 410 GONE（GUEST_PROGRESS_LOCAL_ONLY）。登录管理员的进度数据走 /account/english。
 */
class EnglishLearningIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String KEY = "5bf41b93-274d-4a9f-a7d9-75c8ace8a216";
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicLearningEndpointsAreFencedToGone() throws Exception {
        mockMvc.perform(get("/api/v1/public/english/learning/summary").header("X-Learner-Key", KEY))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
        mockMvc.perform(get("/api/v1/public/english/learning/insights").header("X-Learner-Key", KEY))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
        mockMvc.perform(get("/api/v1/public/english/learning/records/GRAMMAR/1").header("X-Learner-Key", KEY))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
        mockMvc.perform(get("/api/v1/public/english/learning/records/batch")
                        .header("X-Learner-Key", KEY).param("ref", "GRAMMAR:1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
        mockMvc.perform(get("/api/v1/public/english/learning/writing-submissions/1").header("X-Learner-Key", KEY))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
    }

    @Test
    void publicLearningWritesAreFencedToGone() throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/records/GRAMMAR/1")
                        .header("X-Learner-Key", KEY).contentType("application/json")
                        .content("{\"status\":\"COMPLETED\",\"score\":88,\"timeSpentSeconds\":300,\"weakPoints\":[],\"mastery\":0.82}"),
                fetchCsrfToken()))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/writing-submissions/1")
                        .header("X-Learner-Key", KEY).contentType("application/json")
                        .content("{\"bodyText\":\"hello world\",\"status\":\"DRAFT\"}"),
                fetchCsrfToken()))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
    }

    @Test
    void fencePrecedesMissingKeyValidation() throws Exception {
        mockMvc.perform(get("/api/v1/public/english/learning/summary"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GUEST_PROGRESS_LOCAL_ONLY"));
    }
}

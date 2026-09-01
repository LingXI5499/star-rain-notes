package com.starrainnotes.portfolio;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-007 — public portfolio list/detail: published only, featured first,
 * Draft/Withdrawn → 404, case-study detail with adjacent project navigation.
 */
class PortfolioPublicIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanContent() {
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    private Long insertProject(String slug, String status, boolean featured, int sortOrder) {
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, publish_status, project_status,
                     featured, sort_order, published_at)
                VALUES (?, ?, 'summary', '["Java","Spring"]', 'case study body', ?, 'DEVELOPING', ?, ?, ?)
                """, slug, slug, status, featured ? 1 : 0, sortOrder, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void listOnlyPublishedFeaturedFirstBySortOrder() throws Exception {
        insertProject("f-featured", "PUBLISHED", true, 5);
        insertProject("n-2", "PUBLISHED", false, 2);
        insertProject("draft", "DRAFT", true, 1);
        insertProject("n-1", "PUBLISHED", false, 1);

        mockMvc.perform(get("/api/v1/public/portfolio/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].slug").value("f-featured"))
                .andExpect(jsonPath("$[1].slug").value("n-1"))
                .andExpect(jsonPath("$[2].slug").value("n-2"))
                .andExpect(jsonPath("$[0].featured").value(true));
    }

    @Test
    void detailReturnsCaseStudyWithPrevNext() throws Exception {
        insertProject("case-1", "PUBLISHED", true, 1);
        insertProject("case-2", "PUBLISHED", false, 1);
        insertProject("case-3", "PUBLISHED", false, 2);

        mockMvc.perform(get("/api/v1/public/portfolio/projects/case-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMarkdown").value("case study body"))
                .andExpect(jsonPath("$.techStack[0]").value("Java"))
                .andExpect(jsonPath("$.projectStatus").value("DEVELOPING"))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")))
                .andExpect(jsonPath("$.previous.slug").value("case-1"))
                .andExpect(jsonPath("$.next.slug").value("case-3"));
    }

    @Test
    void detailHidesDraftAndWithdrawn() throws Exception {
        insertProject("draft", "DRAFT", false, 1);
        insertProject("withdrawn", "WITHDRAWN", false, 1);

        mockMvc.perform(get("/api/v1/public/portfolio/projects/draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/public/portfolio/projects/withdrawn"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/portfolio/projects/missing"))
                .andExpect(status().isNotFound());
    }
}

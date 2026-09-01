package com.starrainnotes.blog;

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
 * TASK-006 — public blog timeline, filters, tags, calendar, archive,
 * detail with Prev/Next, and the 404 rules for Draft/Withdrawn posts.
 * All time grouping uses the site timezone (Asia/Shanghai).
 */
class BlogPublicIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanContent() {
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
    }

    private Long insertPost(String slug, String status, LocalDateTime publishedAtUtc) {
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'summary', CONCAT('body ', ?), ?, ?)
                """, slug, slug, slug, status, publishedAtUtc);
        return lastId();
    }

    private Long insertTag(String name, String slug) {
        jdbc.update("INSERT INTO blog_tag (name, slug) VALUES (?, ?)", name, slug);
        return lastId();
    }

    private void link(Long postId, Long tagId) {
        jdbc.update("INSERT INTO blog_post_tag (blog_post_id, blog_tag_id) VALUES (?, ?)", postId, tagId);
    }

    private Long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ---------------------------------------------------------------
    // timeline + filters
    // ---------------------------------------------------------------

    @Test
    void timelineOnlyPublishedSortedDesc() throws Exception {
        insertPost("p1", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertPost("p2", "PUBLISHED", LocalDateTime.of(2026, 8, 3, 0, 0));
        insertPost("p3", "DRAFT", null);
        insertPost("p4", "WITHDRAWN", LocalDateTime.of(2026, 8, 2, 0, 0));

        mockMvc.perform(get("/api/v1/public/blog/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].slug").value("p2"))
                .andExpect(jsonPath("$.items[1].slug").value("p1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void tagFilterWorks() throws Exception {
        Long java = insertTag("Java", "java");
        Long spring = insertTag("Spring", "spring");
        Long p1 = insertPost("p1", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        Long p2 = insertPost("p2", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 0, 0));
        link(p1, java);
        link(p1, spring);
        link(p2, java);

        mockMvc.perform(get("/api/v1/public/blog/posts").param("tag", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/api/v1/public/blog/posts").param("tag", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("p1"));
    }

    @Test
    void dateAndMonthFiltersWorkInSiteTimezone() throws Exception {
        // 2026-08-01T18:00Z == 2026-08-02T02:00+08:00 (site day boundary!)
        insertPost("tz-post", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 18, 0));
        insertPost("other", "PUBLISHED", LocalDateTime.of(2026, 8, 5, 0, 0));

        mockMvc.perform(get("/api/v1/public/blog/posts").param("date", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("tz-post"));

        mockMvc.perform(get("/api/v1/public/blog/posts").param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/v1/public/blog/posts").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        // tag + date allowed
        Long java = insertTag("Java", "java");
        link(jdbc.queryForObject("SELECT id FROM blog_post WHERE slug = 'tz-post'", Long.class), java);
        mockMvc.perform(get("/api/v1/public/blog/posts").param("tag", "java").param("date", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        // date + month forbidden
        mockMvc.perform(get("/api/v1/public/blog/posts").param("date", "2026-08-02").param("month", "2026-08"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER_COMBINATION"));
    }

    // ---------------------------------------------------------------
    // detail / 404 / prev-next
    // ---------------------------------------------------------------

    @Test
    void detailReturnsContentAndGlobalPrevNext() throws Exception {
        Long java = insertTag("Java", "java");
        Long p1 = insertPost("p1", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        Long p2 = insertPost("p2", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 0, 0));
        Long p3 = insertPost("p3", "PUBLISHED", LocalDateTime.of(2026, 8, 3, 0, 0));
        link(p2, java);

        mockMvc.perform(get("/api/v1/public/blog/posts/p2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMarkdown").value("body p2"))
                .andExpect(jsonPath("$.tags[0].name").value("Java"))
                .andExpect(jsonPath("$.previous.slug").value("p1"))
                .andExpect(jsonPath("$.next.slug").value("p3"))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")));

        mockMvc.perform(get("/api/v1/public/blog/posts/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.next.slug").value("p2"));
    }

    @Test
    void detailHidesDraftAndWithdrawn() throws Exception {
        insertPost("draft", "DRAFT", null);
        insertPost("withdrawn", "WITHDRAWN", LocalDateTime.of(2026, 8, 1, 0, 0));

        mockMvc.perform(get("/api/v1/public/blog/posts/draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_POST_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/public/blog/posts/withdrawn"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/blog/posts/missing"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // tags / calendar / archive
    // ---------------------------------------------------------------

    @Test
    void publicTagsOnlyCountPublished() throws Exception {
        Long java = insertTag("Java", "java");
        Long spring = insertTag("Spring", "spring");
        Long p1 = insertPost("p1", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        Long p2 = insertPost("p2", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 0, 0));
        Long draft = insertPost("draft", "DRAFT", null);
        link(p1, java);
        link(p2, java);
        link(draft, spring);

        mockMvc.perform(get("/api/v1/public/blog/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].postCount").value(2));
    }

    @Test
    void calendarGroupsBySiteDay() throws Exception {
        // 2026-08-01T18:00Z => site day 2026-08-02
        insertPost("a", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 18, 0));
        insertPost("b", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 4, 0));
        insertPost("c", "PUBLISHED", LocalDateTime.of(2026, 8, 10, 0, 0));
        insertPost("d", "DRAFT", null);

        mockMvc.perform(get("/api/v1/public/blog/calendar").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[0].date").value("2026-08-02"))
                .andExpect(jsonPath("$.days[0].count").value(2))
                .andExpect(jsonPath("$.days[1].date").value("2026-08-10"))
                .andExpect(jsonPath("$.days[1].count").value(1));
    }

    @Test
    void archiveGroupsByYearMonthDesc() throws Exception {
        insertPost("a", "PUBLISHED", LocalDateTime.of(2025, 12, 5, 0, 0));
        insertPost("b", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertPost("c", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 0, 0));
        insertPost("d", "PUBLISHED", LocalDateTime.of(2026, 1, 1, 0, 0));
        insertPost("e", "DRAFT", null);

        mockMvc.perform(get("/api/v1/public/blog/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].months[0].month").value("2026-08"))
                .andExpect(jsonPath("$[0].months[0].count").value(2))
                .andExpect(jsonPath("$[0].months[1].month").value("2026-01"))
                .andExpect(jsonPath("$[1].year").value(2025))
                .andExpect(jsonPath("$[1].months[0].month").value("2025-12"));
    }
}

package com.starrainnotes.search;

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
 * TASK-010 — global search over real business data: published-only, four
 * discriminated sources, type filters (tutorial includes chapters), ranking,
 * LIKE wildcard escaping, counts ignoring the type filter, and immediate
 * disappearance after withdraw.
 */
class SearchIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanContent() {
        jdbc.update("DELETE FROM profile_selected_content");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM portfolio_project");
    }

    @AfterEach
    void cleanUp() {
        cleanContent();
    }

    private Long insertCategory(String slug) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, slug);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertTutorial(Long categoryId, String slug, String title, LocalDateTime updatedAt) {
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at, updated_at)
                VALUES (?, ?, ?, 'summary', 'PUBLISHED', '2026-07-01 00:00:00', ?)
                """, categoryId, title, slug, updatedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertChapter(Long tutorialId, String slug, String title, String body, LocalDateTime updatedAt) {
        jdbc.update("INSERT INTO tutorial_node (tutorial_id, node_type, title) VALUES (?, 'GROUP', ?)",
                tutorialId, title + " 分组");
        Long groupId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
                                           publish_status, published_at, updated_at)
                VALUES (?, ?, 'CHAPTER', ?, ?, 'summary', ?, 'PUBLISHED', '2026-07-01 00:00:00', ?)
                """, tutorialId, groupId, title, slug, body, updatedAt);
    }

    private Long insertBlog(String slug, String title, String body, String status, LocalDateTime publishedAt) {
        LocalDateTime ts = status.equals("DRAFT") ? null : publishedAt;
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'summary', ?, ?, ?)
                """, title, slug, body, status, ts);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertPortfolio(String slug, String title, String body, String status) {
        LocalDateTime ts = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, publish_status, project_status, published_at)
                VALUES (?, ?, 'summary', '[]', ?, ?, 'DEVELOPING', ?)
                """, title, slug, body, status, ts);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void seedSearchData() {
        Long categoryId = insertCategory("backend");
        Long tutorialId = insertTutorial(categoryId, "spring-boot-guide", "Spring Boot 入门教程",
                LocalDateTime.of(2026, 8, 1, 0, 0));
        insertChapter(tutorialId, "quick-start", "Spring Boot 快速开始", "初始化一个 Spring Boot 项目",
                LocalDateTime.of(2026, 8, 2, 0, 0));
        insertBlog("spring-boot-notes", "Spring Boot 实战笔记", "记录 Spring Boot 实践中的踩坑与解决",
                "PUBLISHED", LocalDateTime.of(2026, 8, 3, 0, 0));
        insertPortfolio("spring-boot-blog", "Spring Boot 博客系统", "一个基于 Spring Boot 的博客系统", "PUBLISHED");
        // non-published must never appear
        insertBlog("draft-boot", "Spring Boot 草稿", "草稿内容", "DRAFT", null);
        insertBlog("withdrawn-boot", "Spring Boot 已撤回", "已撤回内容", "WITHDRAWN", LocalDateTime.of(2026, 8, 4, 0, 0));
    }

    // ---------------------------------------------------------------

    @Test
    void searchFindsAllPublishedSources() throws Exception {
        seedSearchData();
        mockMvc.perform(get("/api/v1/public/search").param("q", "spring boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.tutorial").value(1))
                .andExpect(jsonPath("$.counts.chapter").value(1))
                .andExpect(jsonPath("$.counts.blog").value(1))
                .andExpect(jsonPath("$.counts.portfolio").value(1))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[?(@.type=='TUTORIAL')]").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.type=='CHAPTER')]").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.type=='BLOG')]").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.type=='PORTFOLIO')]").isNotEmpty());
    }

    @Test
    void typeTutorialIncludesChapters() throws Exception {
        seedSearchData();
        mockMvc.perform(get("/api/v1/public/search").param("q", "spring boot").param("type", "tutorial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].type").value("CHAPTER"))
                .andExpect(jsonPath("$.items[0].tutorialSlug").value("spring-boot-guide"))
                .andExpect(jsonPath("$.items[0].chapterSlug").value("quick-start"))
                .andExpect(jsonPath("$.items[1].type").value("TUTORIAL"))
                .andExpect(jsonPath("$.items[1].slug").value("spring-boot-guide"));
    }

    @Test
    void typeBlogOnlyBlogs() throws Exception {
        seedSearchData();
        mockMvc.perform(get("/api/v1/public/search").param("q", "spring boot").param("type", "blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].type").value("BLOG"));
    }

    @Test
    void titleExactMatchRanksFirst() throws Exception {
        seedSearchData();
        mockMvc.perform(get("/api/v1/public/search").param("q", "Spring Boot 实战笔记"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("BLOG"))
                .andExpect(jsonPath("$.items[0].title").value("Spring Boot 实战笔记"))
                .andExpect(jsonPath("$.items[0].score").value(100));
    }

    @Test
    void tieBreaksByActivityDesc() throws Exception {
        // both titles contain the query (score 60) -> activityAt DESC decides
        insertBlog("a", "关于 X 技术分享", "a", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertBlog("b", "漫谈 X 技术分享", "b", "PUBLISHED", LocalDateTime.of(2026, 8, 5, 0, 0));

        mockMvc.perform(get("/api/v1/public/search").param("q", "x 技术分享").param("type", "blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].slug").value("b"));
    }

    @Test
    void queryTooShortRejected() throws Exception {
        mockMvc.perform(get("/api/v1/public/search").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/public/search").param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void likeWildcardsAreEscaped() throws Exception {
        insertBlog("progress", "项目进度 100% 完成", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertBlog("control", "项目进度 100X 完成", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertBlog("underscore", "commit a_b 修复", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertBlog("dash", "commit axb 修复", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));

        // literal % must not act as a wildcard (would match "100X 完成" too)
        mockMvc.perform(get("/api/v1/public/search").param("q", "100% 完成").param("type", "blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("progress"));

        // literal _ must not act as a wildcard (would match "axb" too)
        mockMvc.perform(get("/api/v1/public/search").param("q", "a_b").param("type", "blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("underscore"));
    }

    @Test
    void withdrawRemovesFromSearchImmediately() throws Exception {
        Long blogId = insertBlog("temp-post", "临时文章", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        mockMvc.perform(get("/api/v1/public/search").param("q", "临时文章"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        jdbc.update("UPDATE blog_post SET publish_status = 'WITHDRAWN' WHERE id = ?", blogId);

        mockMvc.perform(get("/api/v1/public/search").param("q", "临时文章"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void countsIgnoreTypeFilter() throws Exception {
        seedSearchData();
        mockMvc.perform(get("/api/v1/public/search").param("q", "spring boot").param("type", "blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.counts.tutorial").value(1))
                .andExpect(jsonPath("$.counts.chapter").value(1))
                .andExpect(jsonPath("$.counts.blog").value(1))
                .andExpect(jsonPath("$.counts.portfolio").value(1));
    }

    @Test
    void paginationWorks() throws Exception {
        insertBlog("p1", "Java 并发 一", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 1, 0, 0));
        insertBlog("p2", "Java 并发 二", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 0, 0));
        insertBlog("p3", "Java 并发 三", "body", "PUBLISHED", LocalDateTime.of(2026, 8, 3, 0, 0));

        mockMvc.perform(get("/api/v1/public/search").param("q", "java 并发")
                        .param("page", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/public/search").param("q", "java 并发")
                        .param("page", "2").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("p1"));
    }

    @Test
    void searchFindsVocabularyWordsByEnglishAndChinese() throws Exception {
        jdbc.update("DELETE FROM vocabulary_word");
        jdbc.update("DELETE FROM vocabulary_theme");
        jdbc.update("""
                INSERT INTO vocabulary_theme (layer, layer_order, name, sort_order)
                VALUES ('基础', 1, '天气', 0)
                """);
        Long themeId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO vocabulary_word (theme_id, part_of_speech, word, translation, inflections, examples, sort_order)
                VALUES (?, 'n.', 'apple', '苹果', 'apples', '[]', 0)
                """, themeId);
        jdbc.update("""
                INSERT INTO vocabulary_word (theme_id, part_of_speech, word, translation, inflections, examples, sort_order)
                VALUES (?, 'n.', 'orange', '橙子', 'oranges', '[]', 1)
                """, themeId);

        mockMvc.perform(get("/api/v1/public/search").param("q", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.word").value(1))
                .andExpect(jsonPath("$.items[0].type").value("WORD"))
                .andExpect(jsonPath("$.items[0].title").value("apple"))
                .andExpect(jsonPath("$.items[0].tutorialSlug").value(String.valueOf(themeId)));

        mockMvc.perform(get("/api/v1/public/search").param("q", "苹果").param("type", "word"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("apple"));
    }
}

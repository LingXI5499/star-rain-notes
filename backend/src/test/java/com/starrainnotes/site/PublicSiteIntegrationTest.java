package com.starrainnotes.site;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-004 — public site config and public home data:
 * public-only config (no storage paths/media ids), empty-collection behavior,
 * no draft leakage, latest-updates ordering and featured-project rules.
 */
class PublicSiteIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanContent() {
        cleanTables();
        resetSiteSettings();
    }

    @AfterEach
    void cleanUp() {
        cleanTables();
        resetSiteSettings();
    }

    private void cleanTables() {
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    private void resetSiteSettings() {
        jdbc.update("""
                UPDATE site_setting
                SET site_name = '星雨笔录', tagline = 'Knowledge · Code · Growth',
                    site_url = NULL, footer_text = NULL, github_url = NULL,
                    default_seo_description = '个人知识、技术教程、博客与项目作品记录。',
                    timezone = 'Asia/Shanghai', logo_media_id = NULL, favicon_media_id = NULL
                WHERE id = 1
                """);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Long insertCategory(String slug) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, slug);
        return lastId();
    }

    private Long insertTutorial(Long categoryId, String slug, String status, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, 'summary', ?, ?)
                """, categoryId, slug, slug, status, publishedAt);
        return lastId();
    }

    private Long insertChapter(Long tutorialId, String slug, String status, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title, slug, body_markdown, publish_status, published_at)
                VALUES (?, 'CHAPTER', ?, ?, 'body', ?, ?)
                """, tutorialId, slug, slug, status, publishedAt);
        return lastId();
    }

    private Long insertBlog(String slug, String status, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'summary', 'body', ?, ?)
                """, slug, slug, status, publishedAt);
        return lastId();
    }

    private Long insertPortfolio(String slug, String status, boolean featured, int sortOrder,
                                 LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, publish_status, featured, sort_order, published_at)
                VALUES (?, ?, 'summary', '[]', 'body', ?, ?, ?, ?)
                """, slug, slug, status, featured ? 1 : 0, sortOrder, publishedAt);
        return lastId();
    }

    private Long insertMedia(String storedName, String assetType, String publicUrl) {
        jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url)
                VALUES (?, ?, ?, ?, ?, 100, ?, ?)
                """, assetType, storedName + ".webp", storedName, assetType.equals("IMAGE") ? "image/webp" : "application/pdf",
                assetType.equals("IMAGE") ? "webp" : "pdf", "2026/08/" + storedName + ".webp", publicUrl);
        return lastId();
    }

    private Long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ---------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------

    @Test
    void publicSiteReturnsPublicConfigOnly() throws Exception {
        String body = mockMvc.perform(get("/api/v1/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("星雨笔录"))
                .andExpect(jsonPath("$.tagline").value("Knowledge · Code · Growth"))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.faviconUrl").value(org.hamcrest.Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("storagePath", "storage_path", "logoMediaId", "faviconMediaId", "mediaId");
    }

    @Test
    void publicSiteExposesLogoUrlButNeverStoragePath() throws Exception {
        Long logoId = insertMedia("logo-1", "IMAGE", "/uploads/2026/08/logo-1.webp");
        jdbc.update("UPDATE site_setting SET logo_media_id = ? WHERE id = 1", logoId);

        String body = mockMvc.perform(get("/api/v1/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/uploads/2026/08/logo-1.webp"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("storagePath", "storage_path");
    }

    @Test
    void homeReturnsEmptyCollectionsWhenNoContent() throws Exception {
        mockMvc.perform(get("/api/v1/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestUpdates").isArray())
                .andExpect(jsonPath("$.latestUpdates").isEmpty())
                .andExpect(jsonPath("$.featuredProjects").isArray())
                .andExpect(jsonPath("$.featuredProjects").isEmpty())
                .andExpect(jsonPath("$.aboutPreview").exists());
    }

    @Test
    void homeLatestUpdatesExcludeDraftsAndSortByActivityDesc() throws Exception {
        Long categoryId = insertCategory("home-cat");
        Long tutorialId = insertTutorial(categoryId, "t-home", "PUBLISHED", LocalDateTime.of(2026, 7, 1, 0, 0));
        Long chapterId = insertChapter(tutorialId, "c1", "PUBLISHED", LocalDateTime.of(2026, 7, 1, 0, 0));
        jdbc.update("UPDATE tutorial_node SET updated_at = '2026-08-01 01:00:00' WHERE id = ?", chapterId);
        insertChapter(tutorialId, "c-draft", "DRAFT", null);

        insertBlog("b1", "PUBLISHED", LocalDateTime.of(2026, 8, 2, 2, 0, 0));
        insertBlog("b-draft", "DRAFT", null);

        Long portfolioId = insertPortfolio("p1", "PUBLISHED", false, 0, LocalDateTime.of(2026, 7, 1, 0, 0));
        jdbc.update("UPDATE portfolio_project SET updated_at = '2026-08-03 03:00:00' WHERE id = ?", portfolioId);
        insertPortfolio("p-draft", "DRAFT", false, 0, null);

        String body = mockMvc.perform(get("/api/v1/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestUpdates.length()").value(3))
                .andExpect(jsonPath("$.latestUpdates[0].type").value("PORTFOLIO"))
                .andExpect(jsonPath("$.latestUpdates[0].title").value("p1"))
                .andExpect(jsonPath("$.latestUpdates[1].type").value("BLOG"))
                .andExpect(jsonPath("$.latestUpdates[1].title").value("b1"))
                .andExpect(jsonPath("$.latestUpdates[2].type").value("TUTORIAL"))
                .andExpect(jsonPath("$.latestUpdates[2].title").value("c1"))
                .andExpect(jsonPath("$.latestUpdates[2].tutorialSlug").value("t-home"))
                .andExpect(jsonPath("$.latestUpdates[2].chapterSlug").value("c1"))
                .andReturn().getResponse().getContentAsString();

        // no draft leakage + site-timezone conversion (+08:00)
        assertThat(body)
                .doesNotContain("c-draft", "b-draft", "p-draft")
                .contains("+08:00");
    }

    @Test
    void homeFeaturedProjectsOnlyPublishedAndFeatured() throws Exception {
        insertPortfolio("f1", "PUBLISHED", true, 1, LocalDateTime.of(2026, 7, 1, 0, 0));
        insertPortfolio("f2", "PUBLISHED", true, 0, LocalDateTime.of(2026, 7, 1, 0, 0));
        insertPortfolio("f3", "PUBLISHED", true, 2, LocalDateTime.of(2026, 7, 1, 0, 0));
        insertPortfolio("f-draft", "DRAFT", true, 3, null);
        insertPortfolio("f-normal", "PUBLISHED", false, 4, LocalDateTime.of(2026, 7, 1, 0, 0));

        mockMvc.perform(get("/api/v1/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProjects.length()").value(3))
                .andExpect(jsonPath("$.featuredProjects[0].title").value("f2")) // sort_order 0 first
                .andExpect(jsonPath("$.featuredProjects[1].title").value("f1"))
                .andExpect(jsonPath("$.featuredProjects[2].title").value("f3"));
    }
}

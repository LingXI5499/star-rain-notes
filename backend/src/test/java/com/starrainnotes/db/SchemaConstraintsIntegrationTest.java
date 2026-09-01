package com.starrainnotes.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-002 — verifies the frozen FK / CHECK / UNIQUE / INDEX rules are
 * actually enforced by MySQL (03-database-design.md §6/§8):
 *
 * - PublishStatus semantics: DRAFT => published_at IS NULL,
 *   PUBLISHED/WITHDRAWN => published_at IS NOT NULL
 * - Portfolio publish_status / project_status are independent
 * - ONLINE requires demo_url
 * - profile_selected_content: EXACTLY ONE business FK per row
 * - tutorial_node: CHAPTER is a leaf; parent belongs to the SAME tutorial
 * - GROUP/CHAPTER shape constraint
 * - FK actions: RESTRICT / CASCADE / SET NULL
 *
 * Every test is @Transactional and rolls back; Flyway seed rows (profile id=1)
 * are committed before the test transaction and therefore visible.
 *
 * Rejection assertions use the Spring base type DataAccessException on purpose:
 * MySQL reports CHECK violations as error 3819, which Spring's default
 * SQLExceptionTranslator surfaces as UncategorizedSQLException (a
 * DataAccessException), while UNIQUE (1062) and FK (1452) map to
 * DataIntegrityViolationException. The contract under test is that the
 * database rejects the violating statement, not Spring's translation layer.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaConstraintsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Long insertCategory(String slug) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, slug);
        return lastId();
    }

    private Long insertTutorial(Long categoryId, String slug, String publishStatus, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, categoryId, slug, slug, "summary " + slug, publishStatus, publishedAt);
        return lastId();
    }

    private Long insertChapter(Long tutorialId, String slug) {
        jdbc.update("INSERT INTO tutorial_node (tutorial_id, node_type, title) VALUES (?, 'GROUP', ?)",
                tutorialId, slug + " group");
        Long groupId = lastId();
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, ?, 'CHAPTER', ?, ?, 'body', 'DRAFT')
                """, tutorialId, groupId, slug, slug);
        return lastId();
    }

    private Long insertBlogPost(String slug) {
        jdbc.update("""
                INSERT INTO blog_post (title, slug, summary, body_markdown)
                VALUES (?, ?, 'summary', 'body')
                """, slug, slug);
        return lastId();
    }

    private Long insertMediaAsset(String storedName) {
        jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url)
                VALUES ('IMAGE', ?, ?, 'image/webp', 'webp', 100, ?, ?)
                """, storedName + ".webp", storedName, "2026/08/" + storedName + ".webp",
                "/uploads/2026/08/" + storedName + ".webp");
        return lastId();
    }

    private Long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ---------------------------------------------------------------
    // PublishStatus semantics (DRAFT <=> published_at IS NULL)
    // ---------------------------------------------------------------

    @Test
    void rejectsInvalidPublishStatus() {
        Long categoryId = insertCategory("cat-bogus-status");
        assertThatThrownBy(() -> insertTutorial(categoryId, "t-bogus-status", "BOGUS", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsPublishedWithoutPublishedAt() {
        Long categoryId = insertCategory("cat-pub-null-time");
        assertThatThrownBy(() -> insertTutorial(categoryId, "t-pub-null-time", "PUBLISHED", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDraftWithPublishedAt() {
        Long categoryId = insertCategory("cat-draft-with-time");
        assertThatThrownBy(() -> insertTutorial(categoryId, "t-draft-with-time", "DRAFT", LocalDateTime.now()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void acceptsWithdrawnWithPublishedAt() {
        Long categoryId = insertCategory("cat-withdrawn-ok");
        assertThatCode(() -> insertTutorial(categoryId, "t-withdrawn-ok", "WITHDRAWN", LocalDateTime.now()))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    // Portfolio: independent publish_status / project_status, ONLINE demo
    // ---------------------------------------------------------------

    @Test
    void rejectsInvalidProjectStatus() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, project_status)
                VALUES ('p', 'p-invalid-status', 's', '[]', 'b', 'BOGUS')
                """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsOnlineProjectWithoutDemoUrl() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, project_status, demo_url)
                VALUES ('p', 'p-online-no-demo', 's', '[]', 'b', 'ONLINE', NULL)
                """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void acceptsOnlineProjectWithDemoUrl() {
        assertThatCode(() -> jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown, project_status, demo_url)
                VALUES ('p', 'p-online-with-demo', 's', '[]', 'b', 'ONLINE', 'https://demo.example.com')
                """))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsPublishedProjectWhileStillDeveloping() {
        // PUBLISHED + DEVELOPING is legal: publish_status and project_status are independent.
        assertThatCode(() -> jdbc.update("""
                INSERT INTO portfolio_project
                    (title, slug, summary, tech_stack, body_markdown,
                     publish_status, project_status, published_at)
                VALUES ('p', 'p-published-developing', 's', '[]', 'b',
                        'PUBLISHED', 'DEVELOPING', ?)
                """, LocalDateTime.now()))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    // media_asset: CHECK + storage_path stays a relative key
    // ---------------------------------------------------------------

    @Test
    void rejectsInvalidMediaAssetType() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url)
                VALUES ('VIDEO', 'v.webp', 'v-1', 'video/webp', 'webp', 100,
                        '2026/08/v.webp', '/uploads/2026/08/v.webp')
                """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsZeroDimensionMedia() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO media_asset
                    (asset_type, original_name, stored_name, mime_type, extension,
                     size_bytes, storage_path, public_url, width)
                VALUES ('IMAGE', 'z.webp', 'z-1', 'image/webp', 'webp', 100,
                        '2026/08/z.webp', '/uploads/2026/08/z.webp', 0)
                """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void storesRelativeStorageKey() {
        Long mediaId = insertMediaAsset("rel-key-check");
        String storagePath = jdbc.queryForObject(
                "SELECT storage_path FROM media_asset WHERE id = ?", String.class, mediaId);
        assertThat(storagePath).isEqualTo("2026/08/rel-key-check.webp");
        assertThat(storagePath).doesNotStartWith("/").doesNotStartWith("\\").doesNotContain(":");
    }

    // ---------------------------------------------------------------
    // UNIQUE
    // ---------------------------------------------------------------

    @Test
    void rejectsDuplicateCategorySlug() {
        insertCategory("dup-slug");
        assertThatThrownBy(() -> insertCategory("dup-slug"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDuplicateTutorialSlug() {
        Long categoryId = insertCategory("cat-dup-tutorial");
        insertTutorial(categoryId, "dup-tutorial", "DRAFT", null);
        assertThatThrownBy(() -> insertTutorial(categoryId, "dup-tutorial", "DRAFT", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDuplicateBlogSlug() {
        insertBlogPost("dup-blog");
        assertThatThrownBy(() -> insertBlogPost("dup-blog"))
                .isInstanceOf(DataAccessException.class);
    }

    // ---------------------------------------------------------------
    // FK enforcement
    // ---------------------------------------------------------------

    @Test
    void rejectsTutorialWithMissingCategory() {
        assertThatThrownBy(() -> insertTutorial(999_999L, "t-missing-cat", "DRAFT", null))
                .isInstanceOf(DataAccessException.class);
    }

    // ---------------------------------------------------------------
    // tutorial_node: CHAPTER is a leaf; parent must be in the SAME tutorial
    // ---------------------------------------------------------------

    @Test
    void rejectsChapterWithoutSlug() {
        Long categoryId = insertCategory("cat-node-shape");
        Long tutorialId = insertTutorial(categoryId, "t-node-shape", "DRAFT", null);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title, body_markdown, publish_status)
                VALUES (?, 'CHAPTER', 'no slug', 'body', 'DRAFT')
                """, tutorialId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsGroupWithSlugOrBody() {
        Long categoryId = insertCategory("cat-group-shape");
        Long tutorialId = insertTutorial(categoryId, "t-group-shape", "DRAFT", null);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, 'GROUP', 'bad group', 'g-slug', 'body', 'DRAFT')
                """, tutorialId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsNodeTypeOutsideGroupChapter() {
        Long categoryId = insertCategory("cat-node-type");
        Long tutorialId = insertTutorial(categoryId, "t-node-type", "DRAFT", null);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title)
                VALUES (?, 'VIDEO', 'bad type')
                """, tutorialId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsNodeParentAcrossTutorials() {
        Long categoryId = insertCategory("cat-cross-parent");
        Long tutorialA = insertTutorial(categoryId, "t-cross-a", "DRAFT", null);
        Long tutorialB = insertTutorial(categoryId, "t-cross-b", "DRAFT", null);
        Long nodeA = insertChapter(tutorialA, "a1");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, ?, 'CHAPTER', 'b1', 'b1', 'body', 'DRAFT')
                """, tutorialB, nodeA))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void acceptsNodeParentWithinSameTutorial() {
        Long categoryId = insertCategory("cat-same-parent");
        Long tutorialId = insertTutorial(categoryId, "t-same-parent", "DRAFT", null);
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title)
                VALUES (?, 'GROUP', 'g1')
                """, tutorialId);
        Long groupId = lastId();
        assertThat(groupId).isPositive();

        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, ?, 'CHAPTER', 'c1', 'c1', 'body', 'DRAFT')
                """, tutorialId, groupId);
        Long chapterId = lastId();
        assertThat(chapterId).isPositive();
    }

    // ---------------------------------------------------------------
    // FK actions: RESTRICT / CASCADE / SET NULL
    // ---------------------------------------------------------------

    @Test
    void rejectsNestedKnowledgeSystems() {
        Long parentId = insertCategory("restrict-parent");
        Long childId = insertCategory("restrict-child");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE tutorial_category SET parent_id = ? WHERE id = ?", parentId, childId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsRootChapterAndNestedGroup() {
        Long categoryId = insertCategory("cat-flat-curriculum");
        Long tutorialId = insertTutorial(categoryId, "t-flat-curriculum", "DRAFT", null);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, 'CHAPTER', 'root', 'root', 'body', 'DRAFT')
                """, tutorialId)).isInstanceOf(DataAccessException.class);

        jdbc.update("INSERT INTO tutorial_node (tutorial_id, node_type, title) VALUES (?, 'GROUP', 'root group')",
                tutorialId);
        Long groupId = lastId();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title)
                VALUES (?, ?, 'GROUP', 'nested group')
                """, tutorialId, groupId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsChapterAsChapterParent() {
        Long categoryId = insertCategory("cat-chapter-parent");
        Long tutorialId = insertTutorial(categoryId, "t-chapter-parent", "DRAFT", null);
        Long chapterId = insertChapter(tutorialId, "parent-chapter");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status)
                VALUES (?, ?, 'CHAPTER', 'child', 'child', 'body', 'DRAFT')
                """, tutorialId, chapterId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void cascadesBlogPostTagWhenPostDeleted() {
        Long postId = insertBlogPost("cascade-post");
        jdbc.update("INSERT INTO blog_tag (name, slug) VALUES ('java', 'java')");
        Long tagId = lastId();
        jdbc.update("INSERT INTO blog_post_tag (blog_post_id, blog_tag_id) VALUES (?, ?)", postId, tagId);

        jdbc.update("DELETE FROM blog_post WHERE id = ?", postId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blog_post_tag WHERE blog_post_id = ?", Integer.class, postId);
        assertThat(remaining).isZero();
    }

    @Test
    void setsNullMediaReferenceWhenMediaDeleted() {
        Long mediaId = insertMediaAsset("setnull-media");
        jdbc.update("UPDATE site_setting SET logo_media_id = ? WHERE id = 1", mediaId);

        jdbc.update("DELETE FROM media_asset WHERE id = ?", mediaId);

        Integer logo = jdbc.queryForObject(
                "SELECT logo_media_id FROM site_setting WHERE id = 1", Integer.class);
        assertThat(logo).isNull();
    }

    // ---------------------------------------------------------------
    // profile_selected_content: EXACTLY ONE business FK
    // ---------------------------------------------------------------

    @Test
    void rejectsSelectedContentWithTwoFks() {
        Long categoryId = insertCategory("cat-sel-two");
        Long tutorialId = insertTutorial(categoryId, "t-sel-two", "DRAFT", null);
        Long blogId = insertBlogPost("sel-two");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO profile_selected_content (profile_id, tutorial_id, blog_post_id)
                VALUES (1, ?, ?)
                """, tutorialId, blogId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsSelectedContentWithNoFk() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO profile_selected_content (profile_id, tutorial_id, blog_post_id, portfolio_project_id)
                VALUES (1, NULL, NULL, NULL)
                """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void acceptsSelectedContentWithExactlyOneFk() {
        Long categoryId = insertCategory("cat-sel-one");
        Long tutorialId = insertTutorial(categoryId, "t-sel-one", "DRAFT", null);
        assertThatCode(() -> jdbc.update("""
                INSERT INTO profile_selected_content (profile_id, tutorial_id)
                VALUES (1, ?)
                """, tutorialId))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateSelectedContentPerTarget() {
        Long categoryId = insertCategory("cat-sel-dup");
        Long tutorialId = insertTutorial(categoryId, "t-sel-dup", "DRAFT", null);
        jdbc.update("INSERT INTO profile_selected_content (profile_id, tutorial_id) VALUES (1, ?)", tutorialId);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO profile_selected_content (profile_id, tutorial_id) VALUES (1, ?)", tutorialId))
                .isInstanceOf(DataAccessException.class);
    }

    // ---------------------------------------------------------------
    // Singleton CHECK constraints
    // ---------------------------------------------------------------

    @Test
    void rejectsSecondSiteSettingRow() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO site_setting (id, site_name) VALUES (2, 'other')
                """))
                .isInstanceOf(DataAccessException.class);
    }
}

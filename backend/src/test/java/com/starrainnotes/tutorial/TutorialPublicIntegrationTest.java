package com.starrainnotes.tutorial;

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
 * TASK-005A — public tutorial endpoints:
 * empty-branch pruning of the public category tree, published-only lists,
 * descendant categorySlug filtering, detail metadata/curriculum with existing
 * node data, 404 for Draft/Withdrawn, and zero-chapter tutorials.
 */
class TutorialPublicIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanContent() {
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
    }

    private Long insertCategory(String slug, Long parentId) {
        jdbc.update("INSERT INTO tutorial_category (name, slug, parent_id) VALUES (?, ?, ?)",
                slug, slug, parentId);
        return lastId();
    }

    private Long insertTutorial(Long categoryId, String slug, String status) {
        // PUBLISHED / WITHDRAWN require a non-null published_at (frozen CHECK)
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, 'summary', ?, ?)
                """, categoryId, slug, slug, status, publishedAt);
        return lastId();
    }

    private void insertGroup(Long tutorialId, Long parentId, String title) {
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title)
                VALUES (?, ?, 'GROUP', ?)
                """, tutorialId, parentId, title);
    }

    private void insertChapter(Long tutorialId, Long parentId, String slug, String status) {
        // PUBLISHED / WITHDRAWN require a non-null published_at (frozen CHECK)
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO tutorial_node (tutorial_id, parent_id, node_type, title, slug, body_markdown, publish_status, published_at)
                VALUES (?, ?, 'CHAPTER', ?, ?, 'body', ?, ?)
                """, tutorialId, parentId, slug, slug, status, publishedAt);
    }

    private Long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void publicTreeKeepsOnlyBranchesWithPublishedContent() throws Exception {
        Long emptyRoot = insertCategory("empty-root", null);
        Long emptyChild = insertCategory("empty-child", emptyRoot);
        Long withPublished = insertCategory("has-published", emptyRoot);
        Long draftOnly = insertCategory("draft-only", emptyRoot);
        Long publishedRoot = insertCategory("pub-root", null);

        insertTutorial(withPublished, "t-pub-1", "PUBLISHED");
        insertTutorial(draftOnly, "t-draft", "DRAFT");
        insertTutorial(publishedRoot, "t-pub-2", "PUBLISHED");

        String body = mockMvc.perform(get("/api/v1/public/tutorial-categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("empty-root"))
                .andExpect(jsonPath("$[0].children.length()").value(1))
                .andExpect(jsonPath("$[0].children[0].slug").value("has-published"))
                .andExpect(jsonPath("$[1].slug").value("pub-root"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("draft-only", "empty-child");
    }

    @Test
    void publicTutorialListOnlyPublished() throws Exception {
        Long categoryId = insertCategory("cat", null);
        insertTutorial(categoryId, "t-pub", "PUBLISHED");
        insertTutorial(categoryId, "t-draft", "DRAFT");
        insertTutorial(categoryId, "t-withdrawn", "WITHDRAWN");

        mockMvc.perform(get("/api/v1/public/tutorials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("t-pub"))
                .andExpect(jsonPath("$[0].categoryName").value("cat"))
                .andExpect(jsonPath("$[0].publishedChapterCount").value(0));
    }

    @Test
    void publicListCategorySlugIncludesDescendants() throws Exception {
        Long root = insertCategory("root-cat", null);
        Long child = insertCategory("child-cat", root);
        Long other = insertCategory("other-cat", null);
        insertTutorial(child, "t-in-child", "PUBLISHED");
        insertTutorial(other, "t-in-other", "PUBLISHED");

        // filter by root includes the descendant's tutorial
        mockMvc.perform(get("/api/v1/public/tutorials").param("categorySlug", "root-cat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("t-in-child"));

        mockMvc.perform(get("/api/v1/public/tutorials").param("categorySlug", "child-cat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("t-in-child"));
    }

    @Test
    void publicListUnknownCategoryReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/tutorials").param("categorySlug", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void publicDetailReturnsMetadataAndPublicCurriculum() throws Exception {
        Long categoryId = insertCategory("java", null);
        Long tutorialId = insertTutorial(categoryId, "spring-boot", "PUBLISHED");
        insertGroup(tutorialId, null, "基础");
        Long groupId = lastId();
        insertChapter(tutorialId, groupId, "intro", "PUBLISHED");
        insertChapter(tutorialId, groupId, "draft-chapter", "DRAFT");

        mockMvc.perform(get("/api/v1/public/tutorials/spring-boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("spring-boot"))
                .andExpect(jsonPath("$.categoryPath[0].slug").value("java"))
                .andExpect(jsonPath("$.publishedChapterCount").value(1))
                .andExpect(jsonPath("$.firstChapter.slug").value("intro"))
                .andExpect(jsonPath("$.curriculum[0].type").value("GROUP"))
                .andExpect(jsonPath("$.curriculum[0].children.length()").value(1))
                .andExpect(jsonPath("$.curriculum[0].children[0].slug").value("intro"))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")));
    }

    @Test
    void publicDetailZeroChaptersAllowed() throws Exception {
        Long categoryId = insertCategory("cat", null);
        insertTutorial(categoryId, "t-empty", "PUBLISHED");

        mockMvc.perform(get("/api/v1/public/tutorials/t-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedChapterCount").value(0))
                .andExpect(jsonPath("$.firstChapter").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.curriculum").isEmpty());
    }

    @Test
    void publicDetailDraftReturns404() throws Exception {
        Long categoryId = insertCategory("cat", null);
        insertTutorial(categoryId, "t-draft", "DRAFT");
        mockMvc.perform(get("/api/v1/public/tutorials/t-draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_FOUND"));
    }

    @Test
    void publicDetailWithdrawnReturns404() throws Exception {
        Long categoryId = insertCategory("cat", null);
        insertTutorial(categoryId, "t-withdrawn", "WITHDRAWN");
        mockMvc.perform(get("/api/v1/public/tutorials/t-withdrawn"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_FOUND"));
    }

    @Test
    void publicDetailMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/tutorials/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_FOUND"));
    }

    // ---------------------------------------------------------------
    // chapter reader
    // ---------------------------------------------------------------

    @Test
    void publicChapterReturnsContentWithBreadcrumbs() throws Exception {
        Long categoryId = insertCategory("java", null);
        Long tutorialId = insertTutorial(categoryId, "spring-boot", "PUBLISHED");
        insertGroup(tutorialId, null, "基础");
        Long groupId = lastId();
        insertChapter(tutorialId, groupId, "intro", "PUBLISHED");

        mockMvc.perform(get("/api/v1/public/tutorials/spring-boot/chapters/intro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterSlug").value("intro"))
                .andExpect(jsonPath("$.bodyMarkdown").value("body"))
                .andExpect(jsonPath("$.tutorialSlug").value("spring-boot"))
                .andExpect(jsonPath("$.breadcrumbs[0].type").value("TUTORIAL"))
                .andExpect(jsonPath("$.breadcrumbs[1].type").value("GROUP"))
                .andExpect(jsonPath("$.breadcrumbs[2].type").value("CHAPTER"))
                .andExpect(jsonPath("$.previous").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.next").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")));
    }

    @Test
    void publicChapterPrevNextFollowPreorderAndSkipHidden() throws Exception {
        Long categoryId = insertCategory("cat", null);
        Long tutorialId = insertTutorial(categoryId, "t-prevnext", "PUBLISHED");
        insertChapter(tutorialId, null, "c1", "PUBLISHED");
        insertChapter(tutorialId, null, "c2", "DRAFT");
        insertChapter(tutorialId, null, "c3", "PUBLISHED");
        insertChapter(tutorialId, null, "c4", "WITHDRAWN");
        insertChapter(tutorialId, null, "c5", "PUBLISHED");

        mockMvc.perform(get("/api/v1/public/tutorials/t-prevnext/chapters/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.next.chapterSlug").value("c3"));

        mockMvc.perform(get("/api/v1/public/tutorials/t-prevnext/chapters/c3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.chapterSlug").value("c1"))
                .andExpect(jsonPath("$.next.chapterSlug").value("c5"));

        mockMvc.perform(get("/api/v1/public/tutorials/t-prevnext/chapters/c5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.chapterSlug").value("c3"))
                .andExpect(jsonPath("$.next").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void publicChapterInDraftTutorialReturns404() throws Exception {
        Long categoryId = insertCategory("cat", null);
        Long tutorialId = insertTutorial(categoryId, "t-draft", "DRAFT");
        insertChapter(tutorialId, null, "c1", "PUBLISHED");

        mockMvc.perform(get("/api/v1/public/tutorials/t-draft/chapters/c1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_FOUND"));
    }

    @Test
    void publicDraftChapterReturns404() throws Exception {
        Long categoryId = insertCategory("cat", null);
        Long tutorialId = insertTutorial(categoryId, "t-pub", "PUBLISHED");
        insertChapter(tutorialId, null, "c-draft", "DRAFT");

        mockMvc.perform(get("/api/v1/public/tutorials/t-pub/chapters/c-draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAPTER_NOT_FOUND"));
    }

    @Test
    void publicMissingChapterReturns404() throws Exception {
        Long categoryId = insertCategory("cat", null);
        insertTutorial(categoryId, "t-pub", "PUBLISHED");
        mockMvc.perform(get("/api/v1/public/tutorials/t-pub/chapters/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAPTER_NOT_FOUND"));
    }

    @Test
    void publicCurriculumHidesGroupsWithoutPublishedChapters() throws Exception {
        Long categoryId = insertCategory("cat", null);
        Long tutorialId = insertTutorial(categoryId, "t-groups", "PUBLISHED");
        insertGroup(tutorialId, null, "empty-group");
        Long emptyGroupId = lastId();
        insertChapter(tutorialId, emptyGroupId, "draft-only", "DRAFT");
        insertGroup(tutorialId, null, "full-group");
        Long fullGroupId = lastId();
        insertChapter(tutorialId, fullGroupId, "pub-chapter", "PUBLISHED");

        mockMvc.perform(get("/api/v1/public/tutorials/t-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculum.length()").value(1))
                .andExpect(jsonPath("$.curriculum[0].title").value("full-group"))
                .andExpect(jsonPath("$.curriculum[0].children[0].slug").value("pub-chapter"));
    }
}

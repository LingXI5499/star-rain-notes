package com.starrainnotes.tutorial;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fixed two-level curriculum API, ordering and publish rules. */
class TutorialNodeAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        cleanDomain();
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
    }

    @AfterEach
    void cleanUp() {
        cleanDomain();
        jdbc.update("DELETE FROM admin_user");
    }

    private void cleanDomain() {
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'CHAPTER'");
        jdbc.update("DELETE FROM tutorial_node WHERE node_type = 'GROUP'");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("DELETE FROM tutorial_category");
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"),
                        fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private Long createTutorial(String slug, String status) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, "cat-" + slug);
        Long categoryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at) "
                + "VALUES (?, ?, ?, 'summary', ?, ?)", categoryId, slug, slug, status, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long createGroup(MockHttpSession session, Long tutorialId, String title) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/groups",
                        "{\"title\":\"" + title + "\"}"), csrf(session)).session(session))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createChapter(MockHttpSession session, Long tutorialId, Long groupId, String slug,
                               String markdown) throws Exception {
        String body = "{\"title\":\"" + slug + "\",\"slug\":\"" + slug + "\",\"groupId\":"
                + groupId + ",\"summary\":\"s\",\"bodyMarkdown\":\"" + markdown + "\"}";
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters",
                        body), csrf(session)).session(session))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Integer sortOrderOf(Long nodeId) {
        return jdbc.queryForObject("SELECT sort_order FROM tutorial_node WHERE id = ?", Integer.class, nodeId);
    }

    @Test
    void curriculumReturnsTutorialGroupsAndDirectChaptersWithoutMarkdown() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("curriculum", "DRAFT");
        Long basics = createGroup(session, tutorialId, "基础");
        createGroup(session, tutorialId, "进阶");
        createChapter(session, tutorialId, basics, "hello", "secret-markdown");

        String response = mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/curriculum").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorial.id").value(tutorialId))
                .andExpect(jsonPath("$.tutorial.categoryName").value("curriculum"))
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[0].title").value("基础"))
                .andExpect(jsonPath("$.groups[0].chapterCount").value(1))
                .andExpect(jsonPath("$.groups[0].publishedChapterCount").value(0))
                .andExpect(jsonPath("$.groups[0].chapters[0].groupId").value(basics))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("secret-markdown", "bodyMarkdown");
    }

    @Test
    void chapterCreationRequiresAGroupAndRejectsWrongParents() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialA = createTutorial("parent-a", "DRAFT");
        Long tutorialB = createTutorial("parent-b", "DRAFT");
        Long groupA = createGroup(session, tutorialA, "A");
        Long chapterA = createChapter(session, tutorialA, groupA, "chapter-a", "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialB + "/chapters",
                        "{\"title\":\"x\",\"slug\":\"x\",\"summary\":\"s\",\"bodyMarkdown\":\"b\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialB + "/chapters",
                        chapterJson(groupA, "cross")), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_CROSS_TUTORIAL"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialA + "/chapters",
                        chapterJson(chapterA, "child")), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_NOT_GROUP"));
    }

    private String chapterJson(Long groupId, String slug) {
        return "{\"title\":\"" + slug + "\",\"slug\":\"" + slug + "\",\"groupId\":" + groupId
                + ",\"summary\":\"s\",\"bodyMarkdown\":\"b\"}";
    }

    @Test
    void groupAndChapterCreationAppendAndDuplicateSlugIsRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("append", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1");
        Long g2 = createGroup(session, tutorialId, "g2");
        Long c1 = createChapter(session, tutorialId, g1, "c1", "body");
        Long c2 = createChapter(session, tutorialId, g1, "c2", "body");
        assertThat(sortOrderOf(g1)).isEqualTo(10);
        assertThat(sortOrderOf(g2)).isEqualTo(20);
        assertThat(sortOrderOf(c1)).isEqualTo(10);
        assertThat(sortOrderOf(c2)).isEqualTo(20);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters",
                        chapterJson(g2, "c1")), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
    }

    @Test
    void chapterDetailUpdatesMarkdownWhileCurriculumStaysLightweight() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("markdown", "DRAFT");
        Long group = createGroup(session, tutorialId, "group");
        Long chapter = createChapter(session, tutorialId, group, "intro", "old body");

        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapter)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"slug\":\"intro\",\"summary\":\"new\","
                                + "\"bodyMarkdown\":\"new body\"}"), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.bodyMarkdown").value("new body"))
                .andExpect(jsonPath("$.groupId").value(group))
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"));

        String curriculum = mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/curriculum")
                        .session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(curriculum).doesNotContain("new body", "bodyMarkdown");
    }

    @Test
    void groupAndChapterMovesOnlyReorderTheirOwnSiblingList() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("move", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1");
        Long g2 = createGroup(session, tutorialId, "g2");
        Long c1 = createChapter(session, tutorialId, g1, "c1", "body");
        Long c2 = createChapter(session, tutorialId, g1, "c2", "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/groups/" + g2 + "/move",
                        "{\"targetIndex\":0}"), csrf(session)).session(session)).andExpect(status().isNoContent());
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + c2 + "/move",
                        "{\"targetIndex\":0}"), csrf(session)).session(session)).andExpect(status().isNoContent());

        assertThat(sortOrderOf(g2)).isEqualTo(10);
        assertThat(sortOrderOf(g1)).isEqualTo(20);
        assertThat(sortOrderOf(c2)).isEqualTo(10);
        assertThat(sortOrderOf(c1)).isEqualTo(20);
    }

    @Test
    void compatibilityMoveRejectsNestedGroupsAndCrossGroupChapterDrag() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("compat", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1");
        Long g2 = createGroup(session, tutorialId, "g2");
        Long chapter = createChapter(session, tutorialId, g1, "c1", "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + g1 + "/move",
                        "{\"targetParentId\":" + g2 + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GROUP_NESTING_FORBIDDEN"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + chapter + "/move",
                        "{\"targetParentId\":" + g2 + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CROSS_GROUP_DRAG_FORBIDDEN"));
    }

    @Test
    void explicitReassignmentAppendsAndNormalizesBothGroups() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("reassign", "DRAFT");
        Long source = createGroup(session, tutorialId, "source");
        Long target = createGroup(session, tutorialId, "target");
        Long moved = createChapter(session, tutorialId, source, "moved", "body");
        Long stays = createChapter(session, tutorialId, source, "stays", "body");
        Long existing = createChapter(session, tutorialId, target, "existing", "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + moved
                        + "/reassign", "{\"targetGroupId\":" + target + "}"), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT parent_id FROM tutorial_node WHERE id=?", Long.class, moved))
                .isEqualTo(target);
        assertThat(sortOrderOf(stays)).isEqualTo(10);
        assertThat(sortOrderOf(existing)).isEqualTo(10);
        assertThat(sortOrderOf(moved)).isEqualTo(20);
    }

    @Test
    void deleteRulesAndPublishLifecycleRemainProtected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("lifecycle", "DRAFT");
        Long occupied = createGroup(session, tutorialId, "occupied");
        Long chapter = createChapter(session, tutorialId, occupied, "chapter", "body");

        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorials/" + tutorialId + "/groups/" + occupied),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GROUP_HAS_CHILDREN"));
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapter + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_PUBLISHED"));
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/publish"), csrf(session))
                        .session(session)).andExpect(status().isOk());
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapter + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
        java.sql.Timestamp publishedAt = jdbc.queryForObject(
                "SELECT published_at FROM tutorial_node WHERE id=?", java.sql.Timestamp.class, chapter);
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapter + "/withdraw"),
                        csrf(session)).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishStatus").value("WITHDRAWN"));
        assertThat(jdbc.queryForObject("SELECT published_at FROM tutorial_node WHERE id=?",
                java.sql.Timestamp.class, chapter)).isEqualTo(publishedAt);
    }

    @Test
    void unauthenticatedCurriculumAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tutorials/1/curriculum"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

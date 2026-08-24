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

/**
 * TASK-005B — tutorial node tree, group/chapter management and move rules:
 * cycle protection, chapter-as-parent rejection, cross-tutorial parents,
 * 0-based index moves with sibling sortOrder normalization, chapter publish
 * requires a published tutorial, publishedAt immutability.
 */
class TutorialNodeAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("UPDATE tutorial_node SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_node");
        jdbc.update("DELETE FROM tutorial");
        jdbc.update("UPDATE tutorial_category SET parent_id = NULL");
        jdbc.update("DELETE FROM tutorial_category");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), fetchCsrfToken()))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private Long createTutorial(String slug, String status) {
        jdbc.update("INSERT INTO tutorial_category (name, slug) VALUES (?, ?)", slug, "cat-" + slug);
        Long categoryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        LocalDateTime publishedAt = status.equals("DRAFT") ? null : LocalDateTime.of(2026, 7, 1, 0, 0);
        jdbc.update("""
                INSERT INTO tutorial (category_id, title, slug, summary, publish_status, published_at)
                VALUES (?, ?, ?, 'summary', ?, ?)
                """, categoryId, slug, slug, status, publishedAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long createGroup(MockHttpSession session, Long tutorialId, String title, Long parentId) throws Exception {
        String body = "{\"title\":\"" + title + "\","
                + (parentId == null ? "\"parentId\":null" : "\"parentId\":" + parentId) + "}";
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/groups", body),
                        csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createChapter(MockHttpSession session, Long tutorialId, String slug, Long parentId,
                               String bodyMarkdown) throws Exception {
        String body = "{\"title\":\"" + slug + "\",\"slug\":\"" + slug + "\","
                + (parentId == null ? "\"parentId\":null" : "\"parentId\":" + parentId) + ","
                + "\"summary\":\"s\",\"bodyMarkdown\":\"" + bodyMarkdown + "\"}";
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters", body),
                        csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void move(MockHttpSession session, Long tutorialId, Long nodeId, Long targetParentId, int targetIndex)
            throws Exception {
        String body = "{\"targetParentId\":" + (targetParentId == null ? "null" : targetParentId)
                + ",\"targetIndex\":" + targetIndex + "}";
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + nodeId + "/move", body),
                        csrf(session)).session(session))
                .andExpect(status().isNoContent());
    }

    private Integer sortOrderOf(Long nodeId) {
        return jdbc.queryForObject("SELECT sort_order FROM tutorial_node WHERE id = ?", Integer.class, nodeId);
    }

    // ---------------------------------------------------------------
    // tree / CRUD
    // ---------------------------------------------------------------

    @Test
    void treeReturnsNestedNodesWithoutMarkdown() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-tree", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long g2 = createGroup(session, tutorialId, "g2", g1);
        createChapter(session, tutorialId, "c1", g2, "secret-body");

        String body = mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/nodes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andExpect(jsonPath("$[0].children[0].type").value("GROUP"))
                .andExpect(jsonPath("$[0].children[0].children[0].type").value("CHAPTER"))
                .andExpect(jsonPath("$[0].children[0].children[0].slug").value("c1"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("secret-body", "bodyMarkdown");
    }

    @Test
    void createGroupAppendsSiblingOrder() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-order", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long g2 = createGroup(session, tutorialId, "g2", null);
        assertThat(sortOrderOf(g1)).isEqualTo(10);
        assertThat(sortOrderOf(g2)).isEqualTo(20);
    }

    @Test
    void createChapterUnderChapterRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-leaf", "DRAFT");
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters",
                        "{\"title\":\"c2\",\"slug\":\"c2\",\"parentId\":" + c1
                                + ",\"summary\":\"s\",\"bodyMarkdown\":\"b\"}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_NOT_GROUP"));
    }

    @Test
    void createChapterCrossTutorialParentRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialA = createTutorial("t-a", "DRAFT");
        Long tutorialB = createTutorial("t-b", "DRAFT");
        Long groupA = createGroup(session, tutorialA, "ga", null);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialB + "/chapters",
                        "{\"title\":\"cb\",\"slug\":\"cb\",\"parentId\":" + groupA
                                + ",\"summary\":\"s\",\"bodyMarkdown\":\"b\"}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_CROSS_TUTORIAL"));
    }

    @Test
    void createChapterDuplicateSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-dup", "DRAFT");
        createChapter(session, tutorialId, "dup", null, "body");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/chapters",
                        "{\"title\":\"dup\",\"slug\":\"dup\",\"parentId\":null,\"summary\":\"s\",\"bodyMarkdown\":\"b\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
    }

    @Test
    void updateChapterChangesFieldsButNotStatus() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-upd", "DRAFT");
        Long chapterId = createChapter(session, tutorialId, "c1", null, "old body");

        mockMvc.perform(withCsrf(put("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"slug\":\"c1\",\"summary\":\"ns\",\"bodyMarkdown\":\"new body\"}"),
                        csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.bodyMarkdown").value("new body"))
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"));
    }

    @Test
    void chapterDetailReturnsMarkdownWhileTreeDoesNot() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-markdown", "DRAFT");
        Long chapterId = createChapter(session, tutorialId, "c1", null, "markdown-content-42");

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMarkdown").value("markdown-content-42"));

        String tree = mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/nodes").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(tree).doesNotContain("markdown-content-42", "bodyMarkdown");
    }

    @Test
    void deleteGroupWithChildrenRejectedAndEmptyGroupDeletes() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-delgroup", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        createChapter(session, tutorialId, "c1", g1, "body");

        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorials/" + tutorialId + "/groups/" + g1), csrf(session))
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GROUP_HAS_CHILDREN"));

        Long g2 = createGroup(session, tutorialId, "g2", null);
        mockMvc.perform(withCsrf(delete("/api/v1/admin/tutorials/" + tutorialId + "/groups/" + g2), csrf(session))
                        .session(session))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // publish lifecycle
    // ---------------------------------------------------------------

    @Test
    void publishChapterRequiresPublishedTutorial() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-pubrule", "DRAFT");
        Long chapterId = createChapter(session, tutorialId, "c1", null, "body");

        // tutorial is DRAFT -> chapter publish rejected
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TUTORIAL_NOT_PUBLISHED"));

        // publish the tutorial first, then the chapter succeeds
        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.publishedAt").value(org.hamcrest.Matchers.containsString("+08:00")));
    }

    @Test
    void chapterWithdrawAndRepublishKeepFirstPublishedAt() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-cycle", "PUBLISHED");
        Long chapterId = createChapter(session, tutorialId, "c1", null, "body");

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isOk());
        java.sql.Timestamp first = jdbc.queryForObject(
                "SELECT published_at FROM tutorial_node WHERE id = ?", java.sql.Timestamp.class, chapterId);
        assertThat(first).isNotNull();

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/withdraw"),
                        csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("WITHDRAWN"));
        assertThat(publishedAtOf(chapterId)).isEqualTo(first);

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/publish"),
                        csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
        assertThat(publishedAtOf(chapterId)).isEqualTo(first);
    }

    @Test
    void withdrawDraftChapterRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-wd", "PUBLISHED");
        Long chapterId = createChapter(session, tutorialId, "c1", null, "body");

        mockMvc.perform(withCsrf(post("/api/v1/admin/tutorials/" + tutorialId + "/chapters/" + chapterId + "/withdraw"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PUBLISH_TRANSITION"));
    }

    private java.sql.Timestamp publishedAtOf(Long nodeId) {
        return jdbc.queryForObject("SELECT published_at FROM tutorial_node WHERE id = ?",
                java.sql.Timestamp.class, nodeId);
    }

    // ---------------------------------------------------------------
    // move
    // ---------------------------------------------------------------

    @Test
    void moveReordersSiblingsByZeroBasedIndex() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move1", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");
        Long c2 = createChapter(session, tutorialId, "c2", null, "body");

        // root order: [g1(10), c1(20), c2(30)]; move c1 to index 2 -> [g1, c2, c1]
        move(session, tutorialId, c1, null, 2);

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/nodes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("g1"))
                .andExpect(jsonPath("$[1].title").value("c2"))
                .andExpect(jsonPath("$[2].title").value("c1"));
        assertThat(sortOrderOf(c2)).isEqualTo(20);
        assertThat(sortOrderOf(c1)).isEqualTo(30);
    }

    @Test
    void moveIntoGroupAtIndex() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move2", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");
        Long c2 = createChapter(session, tutorialId, "c2", null, "body");

        move(session, tutorialId, c2, g1, 0);

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/nodes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].children.length()").value(1))
                .andExpect(jsonPath("$[0].children[0].title").value("c2"))
                .andExpect(jsonPath("$[1].title").value("c1"));
    }

    @Test
    void moveIntoGroupNormalizesTheFormerSiblingList() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move-source", "DRAFT");
        Long group = createGroup(session, tutorialId, "group", null);
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");
        Long c2 = createChapter(session, tutorialId, "c2", null, "body");

        // Root starts [group(10), c1(20), c2(30)]. Moving c1 into the
        // group leaves c2 at root and must close the 10-point gap.
        move(session, tutorialId, c1, group, 0);

        assertThat(sortOrderOf(c2)).isEqualTo(20);
    }

    @Test
    void moveIndexClampedToEnd() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move3", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");
        Long c2 = createChapter(session, tutorialId, "c2", null, "body");

        move(session, tutorialId, g1, null, 99);

        mockMvc.perform(get("/api/v1/admin/tutorials/" + tutorialId + "/nodes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("c1"))
                .andExpect(jsonPath("$[1].title").value("c2"))
                .andExpect(jsonPath("$[2].title").value("g1"));
    }

    @Test
    void moveUnderChapterRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move4", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long c1 = createChapter(session, tutorialId, "c1", null, "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + g1 + "/move",
                        "{\"targetParentId\":" + c1 + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_NOT_GROUP"));
    }

    @Test
    void moveGroupIntoOwnDescendantRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move5", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);
        Long g2 = createGroup(session, tutorialId, "g2", g1);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + g1 + "/move",
                        "{\"targetParentId\":" + g2 + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NODE_CYCLE"));
    }

    @Test
    void moveSelfParentRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialId = createTutorial("t-move6", "DRAFT");
        Long g1 = createGroup(session, tutorialId, "g1", null);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialId + "/nodes/" + g1 + "/move",
                        "{\"targetParentId\":" + g1 + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NODE_CYCLE"));
    }

    @Test
    void moveCrossTutorialParentRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long tutorialA = createTutorial("t-move-a", "DRAFT");
        Long tutorialB = createTutorial("t-move-b", "DRAFT");
        Long groupA = createGroup(session, tutorialA, "ga", null);
        Long cB = createChapter(session, tutorialB, "cb", null, "body");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/tutorials/" + tutorialB + "/nodes/" + cB + "/move",
                        "{\"targetParentId\":" + groupA + ",\"targetIndex\":0}"), csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARENT_CROSS_TUTORIAL"));
    }

    @Test
    void unauthenticatedNodeAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tutorials/1/nodes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

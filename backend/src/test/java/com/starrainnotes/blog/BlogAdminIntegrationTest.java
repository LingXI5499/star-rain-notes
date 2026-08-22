package com.starrainnotes.blog;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-006 — admin blog tags/posts + the frozen publish lifecycle:
 * T1 create → T2 publish → T3 update → T4 withdraw → T5 republish.
 * publishedAt must remain T2 (first public publish); createdAt stays T1;
 * updatedAt reflects the modification; tag relations commit in the same
 * transaction.
 */
class BlogAdminIntegrationTest extends AbstractAuthIntegrationTest {

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
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
    }

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

    private Long createTag(MockHttpSession session, String name, String slug) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/blog/tags",
                        "{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\"}"), csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createPost(MockHttpSession session, String slug, List<Long> tagIds) throws Exception {
        String tags = tagIds == null || tagIds.isEmpty() ? "[]"
                : tagIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "[", "]"));
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/blog/posts",
                        "{\"title\":\"Post " + slug + "\",\"slug\":\"" + slug + "\","
                                + "\"summary\":\"Summary\",\"bodyMarkdown\":\"Body\",\"tagIds\":" + tags + "}"),
                        csrf(session)).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private java.sql.Timestamp publishedAtOf(Long postId) {
        return jdbc.queryForObject("SELECT published_at FROM blog_post WHERE id = ?",
                java.sql.Timestamp.class, postId);
    }

    private java.sql.Timestamp createdAtOf(Long postId) {
        return jdbc.queryForObject("SELECT created_at FROM blog_post WHERE id = ?",
                java.sql.Timestamp.class, postId);
    }

    private java.sql.Timestamp updatedAtOf(Long postId) {
        return jdbc.queryForObject("SELECT updated_at FROM blog_post WHERE id = ?",
                java.sql.Timestamp.class, postId);
    }

    private List<String> tagNamesOf(Long postId) {
        return jdbc.queryForList("""
                SELECT t.name FROM blog_post_tag bt
                JOIN blog_tag t ON t.id = bt.blog_tag_id
                WHERE bt.blog_post_id = ?
                ORDER BY t.name
                """, String.class, postId);
    }

    // ---------------------------------------------------------------
    // tags
    // ---------------------------------------------------------------

    @Test
    void tagCrudWorks() throws Exception {
        MockHttpSession session = loginSession();
        Long tagId = createTag(session, "Java", "java");

        mockMvc.perform(get("/api/v1/admin/blog/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));

        mockMvc.perform(withCsrf(put("/api/v1/admin/blog/tags/" + tagId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java 21\",\"slug\":\"java-21\"}"), csrf(session)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java 21"));

        mockMvc.perform(withCsrf(delete("/api/v1/admin/blog/tags/" + tagId), csrf(session)).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/blog/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void tagDuplicateNameAndSlugRejected() throws Exception {
        MockHttpSession session = loginSession();
        createTag(session, "Java", "java");
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/blog/tags",
                        "{\"name\":\"Java\",\"slug\":\"java-2\"}"), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TAG_NAME_CONFLICT"));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/blog/tags",
                        "{\"name\":\"Java 2\",\"slug\":\"java\"}"), csrf(session)).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_CONFLICT"));
    }

    // ---------------------------------------------------------------
    // post lifecycle T1..T5
    // ---------------------------------------------------------------

    @Test
    void lifecycleKeepsFirstPublishedAt() throws Exception {
        MockHttpSession session = loginSession();
        Long tagId = createTag(session, "Java", "java");

        // T1 create (draft)
        Long postId = createPost(session, "t1-t5", List.of(tagId));
        assertThat(publishedAtOf(postId)).isNull();
        java.sql.Timestamp createdAt = createdAtOf(postId);
        assertThat(createdAt).isNotNull();

        // T2 publish
        mockMvc.perform(withCsrf(post("/api/v1/admin/blog/posts/" + postId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
        java.sql.Timestamp firstPublishedAt = publishedAtOf(postId);
        assertThat(firstPublishedAt).isNotNull();

        // T3 update (must not touch publishedAt)
        mockMvc.perform(withCsrf(put("/api/v1/admin/blog/posts/" + postId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"slug\":\"t1-t5\",\"summary\":\"S2\","
                                + "\"bodyMarkdown\":\"B2\",\"tagIds\":[" + tagId + "]}"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
        assertThat(publishedAtOf(postId)).isEqualTo(firstPublishedAt);
        assertThat(createdAtOf(postId)).isEqualTo(createdAt);
        assertThat(updatedAtOf(postId)).isAfter(createdAt);

        // T4 withdraw
        mockMvc.perform(withCsrf(post("/api/v1/admin/blog/posts/" + postId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("WITHDRAWN"));
        assertThat(publishedAtOf(postId)).isEqualTo(firstPublishedAt);

        // T5 republish — publishedAt still T2
        mockMvc.perform(withCsrf(post("/api/v1/admin/blog/posts/" + postId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
        assertThat(publishedAtOf(postId)).isEqualTo(firstPublishedAt);
    }

    @Test
    void withdrawDraftRejected() throws Exception {
        MockHttpSession session = loginSession();
        Long postId = createPost(session, "draft-withdraw", null);
        mockMvc.perform(withCsrf(post("/api/v1/admin/blog/posts/" + postId + "/withdraw"), csrf(session))
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PUBLISH_TRANSITION"));
    }

    // ---------------------------------------------------------------
    // tag relations are transactional
    // ---------------------------------------------------------------

    @Test
    void updateReplacesTagRelationsTransactionally() throws Exception {
        MockHttpSession session = loginSession();
        Long java = createTag(session, "Java", "java");
        Long spring = createTag(session, "Spring", "spring");
        Long vue = createTag(session, "Vue", "vue");

        Long postId = createPost(session, "tagged", List.of(java));
        assertThat(tagNamesOf(postId)).containsExactly("Java");

        mockMvc.perform(withCsrf(put("/api/v1/admin/blog/posts/" + postId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tagged\",\"slug\":\"tagged\",\"summary\":\"S\","
                                + "\"bodyMarkdown\":\"B\",\"tagIds\":[" + spring + "," + vue + "," + spring + "]}"),
                        csrf(session)).session(session))
                .andExpect(status().isOk());

        assertThat(tagNamesOf(postId)).containsExactly("Spring", "Vue");
    }

    @Test
    void postWithUnknownTagRejected() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/blog/posts",
                        "{\"title\":\"T\",\"slug\":\"t\",\"summary\":\"S\",\"bodyMarkdown\":\"B\",\"tagIds\":[999999]}"),
                        csrf(session)).session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
    }

    // ---------------------------------------------------------------
    // list / delete / auth
    // ---------------------------------------------------------------

    @Test
    void adminListFiltersAndPaginates() throws Exception {
        MockHttpSession session = loginSession();
        Long postId = createPost(session, "p1", null);
        createPost(session, "p2", null);
        mockMvc.perform(withCsrf(post("/api/v1/admin/blog/posts/" + postId + "/publish"), csrf(session))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/blog/posts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/admin/blog/posts").param("status", "PUBLISHED").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void deletePostSucceeds() throws Exception {
        MockHttpSession session = loginSession();
        Long postId = createPost(session, "to-delete", null);
        mockMvc.perform(withCsrf(delete("/api/v1/admin/blog/posts/" + postId), csrf(session)).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/admin/blog/posts/" + postId).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_POST_NOT_FOUND"));
    }

    @Test
    void unauthenticatedBlogAdminRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/blog/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

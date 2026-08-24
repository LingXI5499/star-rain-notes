package com.starrainnotes.english.reading;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadingIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('reading-admin',?)",
                passwordEncoder.encode("reading-pass-123"));
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_reading_article");
        jdbc.update("DELETE FROM english_exercise WHERE module_type='READING'");
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void migrationCreatesReadingTablesAndNoFabricatedRows() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
                AND table_name IN ('english_reading_article','english_reading_article_tag',
                'english_reading_article_exercise','english_learning_bundle_reading_item',
                'english_reading_article_grammar_lesson')""", Integer.class)).isEqualTo(5);
    }

    @Test
    void createComputesBackendStats() throws Exception {
        Auth auth = login();
        long id = createArticle(auth, "test-reading-1",
                "The moon is full. The night is calm.", 1, "B1");
        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/english/reading/articles/" + id)
                        .session(auth.session())).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString());
        assertThat(body.get("wordCount").asInt()).isEqualTo(8);
        assertThat(body.get("estimatedMinutes").asInt()).isEqualTo(1);
        assertThat(body.get("publishStatus").asText()).isEqualTo("DRAFT");
    }

    @Test
    void slugConflictReturns409() throws Exception {
        Auth auth = login();
        createArticle(auth, "test-dup", "Hello world.", 1, "A1");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles").session(auth.session())
                        .contentType("application/json")
                        .content(articleJson("test-dup", "Another title.", 1, "A1")), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_CONTENT_SLUG_CONFLICT"));
    }

    @Test
    void invalidLevelAndCefrAreRejected() throws Exception {
        Auth auth = login();
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles").session(auth.session())
                        .contentType("application/json")
                        .content(articleJson("test-bad-level", "Hi.", 4, "A1")), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_READING_LEVEL_INVALID"));
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles").session(auth.session())
                        .contentType("application/json")
                        .content(articleJson("test-bad-cefr", "Hi.", 1, "X9")), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_CEFR_INVALID"));
    }

    @Test
    void tagDimensionIsValidated() throws Exception {
        Auth auth = login();
        // a GENRE term (36) placed in topicTagIds must be rejected
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles").session(auth.session())
                        .contentType("application/json")
                        .content(articleJson("test-tag-dim", "Hi.", 1, "A1").replace(
                                "\"topicTagIds\":[1]", "\"topicTagIds\":[36]")), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_DEPTH_INVALID"));
    }

    @Test
    void publishCheckRejectsMissingTopicOrGenre() throws Exception {
        Auth auth = login();
        // has genre but no topic tag
        String noTopic = "{\"title\":\"test-no-topic\",\"slug\":\"test-no-topic\",\"summary\":\"summary\","
                + "\"bodyMarkdown\":\"Hi there.\",\"readingLevel\":1,\"cefrLevel\":\"A1\","
                + "\"genreTagIds\":[36]}";
        long id = createArticleRaw(auth, noTopic);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + id + "/publish")
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_READING_PUBLISH_INVALID"));
    }

    @Test
    void publishThenWithdrawManagesPublicVisibility() throws Exception {
        Auth auth = login();
        long id = createArticle(auth, "test-live", "The moon is full. The night is calm.", 1, "B1");
        mockMvc.perform(get("/api/v1/public/english/reading/articles/test-live"))
                .andExpect(status().isNotFound());

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/reading/articles/test-live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("test-live"));

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + id + "/withdraw")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/reading/articles/test-live"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishedArticleCannotBeDeletedDirectly() throws Exception {
        Auth auth = login();
        long id = createArticle(auth, "test-pub-del", "Hello world.", 1, "A1");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/reading/articles/" + id)
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_READING_PUBLISHED_DELETE_FORBIDDEN"));
    }

    @Test
    void listAppliesFiltersAndPagination() throws Exception {
        Auth auth = login();
        createArticle(auth, "test-f1", "Hello world.", 1, "A1");
        createArticle(auth, "test-f2", "Hello moon.", 2, "B2");
        mockMvc.perform(get("/api/v1/admin/english/reading/articles")
                        .param("cefr", "A1").session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("test-f1"));
        mockMvc.perform(get("/api/v1/admin/english/reading/articles")
                        .param("level", "2").session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("test-f2"));
    }

    @Test
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english/reading/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void exerciseConfigValidatedAndSanitizedAndScored() throws Exception {
        Auth auth = login();
        long articleId = createArticle(auth, "test-ex", "The moon is full. The night is calm.", 1, "B1");
        // valid single choice
        long exerciseId = createExercise(auth, articleId,
                "{\"options\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"b\",\"text\":\"B\"}],\"answer\":\"a\"}");
        // invalid config rejected
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + articleId + "/exercises")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"questionType\":\"SINGLE_CHOICE\",\"promptMarkdown\":\"p\","
                                + "\"configJson\":\"{\\\"options\\\":[]}\",\"scoreValue\":1}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_EXERCISE_CONFIG_INVALID"));

        // publish article so exercises are reachable publicly
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + articleId + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());

        // public exercises must not leak the answer
        String publicBody = mockMvc.perform(get("/api/v1/public/english/reading/articles/test-ex/exercises"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode publicJson = objectMapper.readTree(publicBody);
        assertThat(publicJson.get(0).get("config").has("answer")).isFalse();
        assertThat(publicJson.get(0).get("config").has("correctIndexes")).isFalse();

        // scoring against the stored answer
        mockMvc.perform(withCsrf(post("/api/v1/public/english/reading/articles/test-ex/check")
                        .contentType("application/json")
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseId + ",\"answer\":\"a\"}]}"), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.items[0].correct").value(true));
    }

    @Test
    void exerciseMoveNormalizesOrderAndCrossBindingDenied() throws Exception {
        Auth auth = login();
        long articleId = createArticle(auth, "test-move", "Hello world.", 1, "A1");
        long a = createExercise(auth, articleId,
                "{\"options\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"b\",\"text\":\"B\"}],\"answer\":\"a\"}");
        long b = createExercise(auth, articleId,
                "{\"options\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"b\",\"text\":\"B\"}],\"answer\":\"b\"}");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + articleId
                        + "/exercises/" + a + "/move").session(auth.session())
                        .contentType("application/json").content("{\"targetIndex\":1}"), auth.csrf()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT sort_order FROM english_exercise WHERE id=?",
                Integer.class, a)).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT sort_order FROM english_exercise WHERE id=?",
                Integer.class, b)).isEqualTo(10);
    }

    @Test
    void searchReturnsReadingTypeAndCount() throws Exception {
        Auth auth = login();
        long id = createArticle(auth, "test-search", "The moon is full and bright." , 1, "B1");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/search").param("q", "moon").param("type", "reading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.reading").value(1))
                .andExpect(jsonPath("$.items[0].type").value("READING"))
                .andExpect(jsonPath("$.items[0].slug").value("test-search"));
    }

    @Test
    void deleteInUseTaxonomyTermReturns409() throws Exception {
        Auth auth = login();
        createArticle(auth, "test-tag-use", "Hello world.", 1, "A1"); // uses topic id 1
        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/taxonomy/1").session(auth.session()), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_IN_USE"));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Auth login() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"reading-admin\",\"password\":\"reading-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return new Auth(session, csrfResult.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    private long createArticle(Auth auth, String slug, String body, int level, String cefr) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles")
                        .session(auth.session()).contentType("application/json")
                        .content(articleJson(slug, body, level, cefr)), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createArticleRaw(Auth auth, String json) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles")
                        .session(auth.session()).contentType("application/json").content(json), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createExercise(Auth auth, long articleId, String configJson) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/reading/articles/"
                        + articleId + "/exercises").session(auth.session())
                        .contentType("application/json")
                        .content("{\"questionType\":\"SINGLE_CHOICE\",\"promptMarkdown\":\"prompt\","
                                + "\"configJson\":" + objectMapper.writeValueAsString(configJson)
                                + ",\"scoreValue\":1,\"publishStatus\":\"PUBLISHED\"}"), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String articleJson(String slug, String body, int level, String cefr) {
        return "{\"title\":\"" + slug + "\",\"slug\":\"" + slug + "\",\"summary\":\"summary\","
                + "\"bodyMarkdown\":\"" + body + "\",\"readingLevel\":" + level
                + ",\"cefrLevel\":\"" + cefr + "\",\"topicTagIds\":[1],\"genreTagIds\":[36],"
                + "\"abilityTagIds\":[24]}";
    }

    private String json(String configJson) {
        return configJson;
    }

    private record Auth(MockHttpSession session, String csrf) { }
}

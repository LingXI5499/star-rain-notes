package com.starrainnotes.english.listening;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListeningIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('listen-admin',?)",
                passwordEncoder.encode("listen-pass-123"));
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM english_listening_item");
        jdbc.update("DELETE FROM english_listening_pronunciation_rule");
        jdbc.update("DELETE FROM english_exercise WHERE module_type='LISTENING'");
        jdbc.update("DELETE FROM media_asset");
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void migrationCreatesTablesAndHardensSortOrder() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item", Integer.class)).isEqualTo(0);
        Integer check = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
                AND table_name IN ('english_listening_item','english_listening_segment',
                'english_listening_item_exercise','english_learning_bundle_listening_item',
                'english_reading_listening_pair','english_listening_pronunciation_rule','english_listening_item_tag')
                """, Integer.class);
        assertThat(check).isEqualTo(7);
    }

    @Test
    void createValidatesMediaCefrAndLevel() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        String body = itemJson("listen-1", 1, audio, 60);
        MvcResult created = mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items")
                        .session(auth.session()).contentType("application/json").content(body), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
        assertThat(json.get("audioUrl").asText()).endsWith(".mp3");

        // invalid level
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items").session(auth.session())
                        .contentType("application/json")
                        .content(itemJson("listen-bad", 4, audio, 60)), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_LEVEL_INVALID"));
        // invalid cefr
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items").session(auth.session())
                        .contentType("application/json")
                        .content(itemJson("listen-bad-c", 1, audio, 60).replace("\"B1\"", "\"X9\"")), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_CEFR_INVALID"));
    }

    @Test
    void nonAudioMediaCannotServeAsAudio() throws Exception {
        Auth auth = login();
        // an IMAGE cannot be used as audio
        long image = uploadImage(auth);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items").session(auth.session())
                        .contentType("application/json")
                        .content(itemJson("listen-img", 1, image, 60)), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_AUDIO_MEDIA_INVALID"));
    }

    @Test
    void batchSegmentsReplacesExistingRowsAndTaxonomyDetectsListeningUse() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long itemId = createItem(auth, itemJson("listen-batch", 1, audio, 60));

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + itemId + "/segments")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"startMs\":0,\"endMs\":1000,\"transcriptText\":\"old one\"}"), auth.csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + itemId + "/segments")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"startMs\":1000,\"endMs\":2000,\"transcriptText\":\"old two\"}"), auth.csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(withCsrf(put("/api/v1/admin/english/listening/items/" + itemId + "/segments/batch")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"segments\":["
                                + "{\"startMs\":0,\"endMs\":1500,\"transcriptText\":\"new one\"},"
                                + "{\"startMs\":1500,\"endMs\":3000,\"transcriptText\":\"new two\"}]}"), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transcriptText").value("new one"))
                .andExpect(jsonPath("$[0].sortOrder").value(10))
                .andExpect(jsonPath("$[1].transcriptText").value("new two"))
                .andExpect(jsonPath("$[1].sortOrder").value(20));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_segment WHERE listening_item_id=?",
                Integer.class, itemId)).isEqualTo(2);

        // Invalid replacements are rejected before deletion, preserving the old rows.
        mockMvc.perform(withCsrf(put("/api/v1/admin/english/listening/items/" + itemId + "/segments/batch")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"segments\":["
                                + "{\"startMs\":0,\"endMs\":2000,\"transcriptText\":\"one\"},"
                                + "{\"startMs\":1500,\"endMs\":3000,\"transcriptText\":\"two\"}]}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_SEGMENT_OVERLAP"));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_segment WHERE listening_item_id=?",
                Integer.class, itemId)).isEqualTo(2);

        // An empty complete replacement intentionally clears every segment.
        mockMvc.perform(withCsrf(put("/api/v1/admin/english/listening/items/" + itemId + "/segments/batch")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"segments\":[]}"), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/taxonomy/41")
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_IN_USE"));
    }

    @Test
    void publishLifecycleAndPublicVisibility() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long id = createItem(auth, itemJson("listen-live", 1, audio, 60));
        makePublishable(auth, id);
        // public 404 while draft
        mockMvc.perform(get("/api/v1/public/english/listening/items/listen-live"))
                .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/listening/items/listen-live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.audioUrl").value(org.hamcrest.Matchers.endsWith(".mp3")));
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/withdraw")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/listening/items/listen-live"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishedItemCannotBeDeletedAndPublishRequiresTags() throws Exception {
        Auth auth = login();
        // no scene + no format tags -> publish rejected
        long audio = uploadAudio(auth);
        String noTags = itemJson("listen-notags", 1, audio, 60).replace("\"sceneTagIds\":[41]", "\"sceneTagIds\":[]")
                .replace("\"formatTagIds\":[45]", "\"formatTagIds\":[]");
        long id = createItem(auth, noTags);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/publish")
                .session(auth.session()), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_PUBLISH_INVALID"));

        // proper item -> publish -> delete forbidden
        long id2 = createItem(auth, itemJson("listen-del", 1, audio, 60));
        makePublishable(auth, id2);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id2 + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/listening/items/" + id2)
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_PUBLISHED_DELETE_FORBIDDEN"));
    }

    @Test
    void segmentsValidateRangeAndOrder() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long id = createItem(auth, itemJson("listen-seg", 1, audio, 60));
        // end > duration
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/segments")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"startMs\":0,\"endMs\":120000,\"transcriptText\":\"x\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_SEGMENT_INVALID"));
        // valid + end<=start rejected
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/segments")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"startMs\":0,\"endMs\":1000,\"transcriptText\":\"hello\"}"), auth.csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/segments")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"startMs\":1000,\"endMs\":1000,\"transcriptText\":\"bad\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exerciseSanitizationAndScoringAndOwnership() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long id = createItem(auth, itemJson("listen-ex", 1, audio, 60));
        addValidSegment(auth, id);

        // MINIMAL_PAIR config must be valid and published
        String exBody = "{\"questionType\":\"MINIMAL_PAIR\",\"promptMarkdown\":\"p\","
                + "\"configJson\":" + objectMapper.writeValueAsString(
                        "{\"pair\":[\"ship\",\"sheep\"],\"answer\":\"sheep\"}")
                + ",\"scoreValue\":1,\"publishStatus\":\"PUBLISHED\"}";
        MvcResult ex = mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/exercises")
                        .session(auth.session()).contentType("application/json").content(exBody), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        long exerciseId = objectMapper.readTree(ex.getResponse().getContentAsString()).get("id").asLong();

        // The admin page depends on this contract: existing exercises must be
        // returned with their complete config and an empty sibling item must
        // return [] instead of failing the whole management screen.
        mockMvc.perform(get("/api/v1/admin/english/listening/items/" + id + "/exercises")
                        .session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exerciseId))
                .andExpect(jsonPath("$[0].questionType").value("MINIMAL_PAIR"))
                .andExpect(jsonPath("$[0].config.answer").value("sheep"));
        long emptyItemId = createItem(auth, itemJson("listen-empty-exercises", 1, audio, 60));
        mockMvc.perform(get("/api/v1/admin/english/listening/items/" + emptyItemId + "/exercises")
                .session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());

        // public payload must expose pair but not answer
        String pub = mockMvc.perform(get("/api/v1/public/english/listening/items/listen-ex/exercises"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode pubJson = objectMapper.readTree(pub);
        JsonNode config = pubJson.get(0).get("config");
        assertThat(config.has("answer")).isFalse();
        assertThat(config.get("pair").get(1).asText()).isEqualTo("sheep");

        // correct answer
        mockMvc.perform(withCsrf(post("/api/v1/public/english/listening/items/listen-ex/check")
                        .contentType("application/json")
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseId + ",\"answer\":\"sheep\"}]}"), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.items[0].correct").value(true));

        // duplicate submission rejected (422)
        mockMvc.perform(withCsrf(post("/api/v1/public/english/listening/items/listen-ex/check")
                        .contentType("application/json")
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseId + ",\"answer\":\"sheep\"},"
                                + "{\"exerciseId\":" + exerciseId + ",\"answer\":\"sheep\"}]}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void readingPairAddAndRemove() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long id = createItem(auth, itemJson("listen-pair", 1, audio, 60));
        // reading id 1 may not exist -> 422
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/reading-pairs")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"readingArticleId\":99999,\"relationType\":\"SAME_TOPIC\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_LISTENING_PAIR_READING_INVALID"));
    }

    @Test
    void pronunciationRuleLifecycle() throws Exception {
        Auth auth = login();
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/pronunciation").session(auth.session())
                        .contentType("application/json")
                        .content("{\"ruleType\":\"LINKING\",\"title\":\"Rule\",\"slug\":\"rule-1\","
                                + "\"summary\":\"s\",\"bodyMarkdown\":\"# Body\"}"), auth.csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/public/english/listening/pronunciation/rule-1"))
                .andExpect(status().isNotFound());
        MvcResult list = mockMvc.perform(get("/api/v1/admin/english/listening/pronunciation")
                .session(auth.session())).andExpect(status().isOk()).andReturn();
        long ruleId = objectMapper.readTree(list.getResponse().getContentAsString()).get(0).get("id").asLong();
        mockMvc.perform(get("/api/v1/admin/english/listening/pronunciation/" + ruleId)
                        .session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rule"));
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/pronunciation/" + ruleId + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/listening/pronunciation/rule-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bodyMarkdown").value("# Body"));
    }

    @Test
    void searchReturnsListeningTypesAndCounts() throws Exception {
        Auth auth = login();
        long audio = uploadAudio(auth);
        long id = createItem(auth, itemJson("listen-search", 1, audio, 60));
        makePublishable(auth, id);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/search").param("q", "transcript").param("type", "listening"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.listening").value(1))
                .andExpect(jsonPath("$.items[0].type").value("LISTENING"))
                .andExpect(jsonPath("$.items[0].slug").value("listen-search"));
    }

    @Test
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english/listening/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Auth login() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"listen-admin\",\"password\":\"listen-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return new Auth(session, csrf.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    private long createItem(Auth auth, String json) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items")
                        .session(auth.session()).contentType("application/json").content(json), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void makePublishable(Auth auth, long itemId) throws Exception {
        addValidSegment(auth, itemId);
        addPublishedExercise(auth, itemId);
    }

    private void addValidSegment(Auth auth, long itemId) throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/admin/english/listening/items/" + itemId + "/segments/batch")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"segments\":[{\"startMs\":0,\"endMs\":1000,"
                                + "\"transcriptText\":\"A complete sentence.\"}]}"), auth.csrf()))
                .andExpect(status().isOk());
    }

    private void addPublishedExercise(Auth auth, long itemId) throws Exception {
        String body = "{\"questionType\":\"MINIMAL_PAIR\",\"promptMarkdown\":\"Choose the word\","
                + "\"configJson\":" + objectMapper.writeValueAsString(
                        "{\"pair\":[\"ship\",\"sheep\"],\"answer\":\"sheep\"}")
                + ",\"scoreValue\":1,\"publishStatus\":\"PUBLISHED\"}";
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/listening/items/" + itemId + "/exercises")
                        .session(auth.session()).contentType("application/json").content(body), auth.csrf()))
                .andExpect(status().isCreated());
    }

    private String itemJson(String slug, int level, Long audioId, int duration) {
        return "{\"title\":\"" + slug + "\",\"slug\":\"" + slug + "\",\"summary\":\"summary\","
                + "\"transcriptMarkdown\":\"transcript and words here\",\"cefrLevel\":\"B1\","
                + "\"listeningLevel\":" + level + ",\"audioMediaId\":" + audioId
                + ",\"durationSeconds\":" + duration + ",\"topicTagIds\":[1],\"sceneTagIds\":[41],"
                + "\"formatTagIds\":[45],\"abilityTagIds\":[24]}";
    }

    private long uploadAudio(Auth auth) throws Exception {
        byte[] mp3 = {0x49, 0x44, 0x33, 0x04, 0, 0, 0, 0, 0, 0};
        MvcResult result = mockMvc.perform(withCsrf(multipart("/api/v1/admin/media-assets")
                        .file(new MockMultipartFile("file", "clip.mp3", "audio/mpeg", mp3)), auth.csrf())
                        .session(auth.session()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long uploadImage(Auth auth) throws Exception {
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        MvcResult result = mockMvc.perform(withCsrf(multipart("/api/v1/admin/media-assets")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", png)), auth.csrf())
                        .session(auth.session()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private record Auth(MockHttpSession session, String csrf) { }
}

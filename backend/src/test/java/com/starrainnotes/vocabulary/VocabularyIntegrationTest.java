package com.starrainnotes.vocabulary;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Approved vocabulary module — public theme/word browsing, personal memory
 * +1, and admin example / memory management.
 */
class VocabularyIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminAndClean() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        jdbc.update("DELETE FROM vocabulary_word");
        jdbc.update("DELETE FROM vocabulary_theme");
        jdbc.update("DELETE FROM media_asset WHERE stored_name LIKE 'vocabulary-test-%'");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM vocabulary_word");
        jdbc.update("DELETE FROM vocabulary_theme");
        jdbc.update("DELETE FROM media_asset WHERE stored_name LIKE 'vocabulary-test-%'");
        jdbc.update("DELETE FROM admin_user");
    }

    private long insertTheme(String layer, int layerOrder, String name) {
        jdbc.update("INSERT INTO vocabulary_theme (layer, layer_order, name, sort_order) VALUES (?, ?, ?, ?)",
                layer, layerOrder, name, 1);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertWord(long themeId, String word, String translation) {
        jdbc.update("""
                INSERT INTO vocabulary_word
                    (theme_id, part_of_speech, word, phonetic_us, translation, inflections, examples, sort_order)
                VALUES (?, '名词 n.', ?, '/tɛst/', ?, NULL, '[]', 1)
                """, themeId, word, translation);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
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

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    @Test
    void publicThemesReturnLayersWithWordCounts() throws Exception {
        long themeA = insertTheme("基础通用词层A", 1, "人体与感官");
        long themeB = insertTheme("基础通用词层A", 1, "时间与日期");
        insertWord(themeA, "body", "身体");
        insertWord(themeA, "head", "头");
        insertWord(themeB, "time", "时间");

        mockMvc.perform(get("/api/v1/public/vocabulary/themes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].layer").value("基础通用词层A"))
                .andExpect(jsonPath("$[0].themes.length()").value(2))
                .andExpect(jsonPath("$[0].themes[0].id").value(themeA))
                .andExpect(jsonPath("$[0].themes[0].name").value("人体与感官"))
                .andExpect(jsonPath("$[0].themes[0].wordCount").value(2))
                .andExpect(jsonPath("$[0].themes[1].wordCount").value(1));
    }

    @Test
    void publicThemeWordsArePagedWithCardFields() throws Exception {
        long theme = insertTheme("核心生活场景层A", 2, "家与日常生活");
        insertWord(theme, "sofa", "沙发");
        insertWord(theme, "lamp", "台灯");
        insertWord(theme, "table", "桌子");

        mockMvc.perform(get("/api/v1/public/vocabulary/themes/" + theme + "/words")
                        .param("page", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].word").value("sofa"))
                .andExpect(jsonPath("$.items[0].partOfSpeech").value("名词 n."))
                .andExpect(jsonPath("$.items[0].phoneticUs").value("/tɛst/"))
                .andExpect(jsonPath("$.items[0].translation").value("沙发"))
                .andExpect(jsonPath("$.items[0].memoryCount").value(0))
                .andExpect(jsonPath("$.items[0].examples").isArray());
    }

    @Test
    void rememberedFilterReturnsOnlyMemorizedWords() throws Exception {
        long theme = insertTheme("社会与世界主题层B", 4, "经济与商业");
        long memorized = insertWord(theme, "market", "市场");
        insertWord(theme, "trade", "贸易");
        String csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/public/vocabulary/words/" + memorized + "/memory"), csrf))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/public/vocabulary/themes/" + theme + "/words")
                        .param("remembered", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].word").value("market"))
                .andExpect(jsonPath("$.items[0].memoryCount").value(1));

        mockMvc.perform(get("/api/v1/public/vocabulary/themes/" + theme + "/words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void missingThemeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/vocabulary/themes/999999/words"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOCABULARY_THEME_NOT_FOUND"));
    }

    @Test
    void memoryPlusOnePersistsAndStampsTime() throws Exception {
        long theme = insertTheme("社会与世界主题层A", 3, "城市与社区");
        long wordId = insertWord(theme, "block", "街区");
        // Public state-changing endpoints still require the SPA CSRF header.
        String csrf = fetchCsrfToken();

        mockMvc.perform(withCsrf(post("/api/v1/public/vocabulary/words/" + wordId + "/memory"), csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoryCount").value(1))
                .andExpect(jsonPath("$.lastMemoryAt").isNotEmpty());

        mockMvc.perform(withCsrf(post("/api/v1/public/vocabulary/words/" + wordId + "/memory"), csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoryCount").value(2));

        Integer count = jdbc.queryForObject(
                "SELECT memory_count FROM vocabulary_word WHERE id = ?", Integer.class, wordId);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(2);
    }

    @Test
    void missingWordMemoryReturns404() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/public/vocabulary/words/999999/memory"), fetchCsrfToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOCABULARY_WORD_NOT_FOUND"));
    }

    @Test
    void publicStudyAndBatchEndpointsReturnRequestedWordsInOrder() throws Exception {
        long theme = insertTheme("基础通用词层A", 1, "批量读取");
        long first = insertWord(theme, "firstword", "第一个词");
        long second = insertWord(theme, "secondword", "第二个词");

        mockMvc.perform(get("/api/v1/public/vocabulary/words/" + first + "/study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("firstword"));
        mockMvc.perform(get("/api/v1/public/vocabulary/words/batch")
                        .param("ids", second + "," + first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    @Test
    void adminAddAndRemoveExample() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = csrf(session);
        long theme = insertTheme("语言系统层A", 4, "词根词缀系统");
        long wordId = insertWord(theme, "spect", "看");

        mockMvc.perform(withCsrf(post("/api/v1/admin/vocabulary/words/" + wordId + "/examples")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"sentence\":\"I respect her view.\",\"translation\":\"我尊重她的观点。\"}"), csrf)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples.length()").value(1))
                .andExpect(jsonPath("$.examples[0].sentence").value("I respect her view."));

        mockMvc.perform(withCsrf(delete("/api/v1/admin/vocabulary/words/" + wordId + "/examples/0"), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples.length()").value(0));
    }

    @Test
    void adminUpdateWordAndSetMemory() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = csrf(session);
        long theme = insertTheme("抽象阅读与逻辑层A", 5, "观点与态度");
        long wordId = insertWord(theme, "bias", "偏见");

        mockMvc.perform(withCsrf(put("/api/v1/admin/vocabulary/words/" + wordId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"translation\":\"偏见；偏向\",\"phoneticUs\":\"/ˈbaɪəs/\","
                                + "\"inflections\":\"复数 biases\"}"), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translation").value("偏见；偏向"))
                .andExpect(jsonPath("$.phoneticUs").value("/ˈbaɪəs/"));

        mockMvc.perform(withCsrf(put("/api/v1/admin/vocabulary/words/" + wordId + "/memory")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"memoryCount\":5}"), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoryCount").value(5));
    }

    @Test
    void adminAudioLifecycleIsVisibleOnPublicStudyCard() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = csrf(session);
        long theme = insertTheme("基础通用词层A", 1, "发音测试");
        long wordId = insertWord(theme, "voice", "声音");
        jdbc.update("""
                INSERT INTO media_asset(asset_type,original_name,stored_name,mime_type,extension,
                    size_bytes,storage_path,public_url)
                VALUES ('AUDIO','voice.mp3','vocabulary-test-voice.mp3','audio/mpeg','mp3',128,
                    'test/voice.mp3','/uploads/test/voice.mp3')
                """);
        long mediaId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        MvcResult created = mockMvc.perform(withCsrf(post("/api/v1/admin/vocabulary/words/" + wordId + "/audio")
                        .session(session).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mediaAssetId\":" + mediaId + ",\"accent\":\"US\",\"provider\":\"UPLOADED\"," +
                                "\"licenseNote\":\"本站授权测试音频\",\"primary\":true}"), csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primary").value(true))
                .andReturn();
        long audioId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/public/vocabulary/words/" + wordId + "/study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audios[0].publicUrl").value("/uploads/test/voice.mp3"))
                .andExpect(jsonPath("$.audios[0].licenseNote").value("本站授权测试音频"));

        mockMvc.perform(withCsrf(delete("/api/v1/admin/vocabulary/words/" + wordId + "/audio/" + audioId)
                        .session(session), csrf))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/public/vocabulary/words/" + wordId + "/study"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.audios").isEmpty());
    }

    @Test
    void adminListSupportsThemeAndQueryFilter() throws Exception {
        MockHttpSession session = loginSession();
        String csrf = csrf(session);
        long theme = insertTheme("学习与工作主干层A", 6, "学校与教室");
        insertWord(theme, "blackboard", "黑板");
        insertWord(theme, "chalk", "粉笔");

        mockMvc.perform(withCsrf(get("/api/v1/admin/vocabulary/words").param("themeId", String.valueOf(theme)), csrf)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(withCsrf(get("/api/v1/admin/vocabulary/words").param("q", "粉笔"), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].word").value("chalk"));
    }

    @Test
    void unauthenticatedAdminVocabularyRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/vocabulary/words"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

package com.starrainnotes.english.grammar;

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

class EnglishGrammarIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('grammar-admin',?)",
                passwordEncoder.encode("grammar-pass-123"));
        cleanupTemporaryRows();
        jdbc.update("UPDATE english_grammar_course SET publish_status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6)) WHERE id=1");
        jdbc.update("UPDATE english_grammar_lesson SET publish_status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6)) WHERE id<=42");
    }

    @AfterEach
    void cleanup() {
        cleanupTemporaryRows();
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("UPDATE english_grammar_course SET publish_status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6)) WHERE id=1");
        jdbc.update("UPDATE english_grammar_lesson SET publish_status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6)) WHERE id<=42");
    }

    private void cleanupTemporaryRows() {
        jdbc.update("DELETE FROM english_grammar_lesson WHERE id>42");
        jdbc.update("DELETE FROM english_grammar_section WHERE id>10");
    }

    @Test
    void migrationSeedsOneCourseTenSectionsAndFortyTwoLessons() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_course", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_section", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson", Integer.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE publish_status='PUBLISHED'", Integer.class)).isEqualTo(42);
    }

    @Test
    void publicCurriculumAndLessonExposeFixedTwoLevelsAndNavigation() throws Exception {
        mockMvc.perform(get("/api/v1/public/english/grammar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.title").value("英语语法完整教程"))
                .andExpect(jsonPath("$.sections.length()").value(10))
                .andExpect(jsonPath("$.sections[0].lessons.length()").value(2));

        mockMvc.perform(get("/api/v1/public/english/grammar/lessons/1-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("语言结构基础"))
                .andExpect(jsonPath("$.bodyMarkdown").value(org.hamcrest.Matchers.startsWith("## 学习内容")))
                .andExpect(jsonPath("$.previous").doesNotExist())
                .andExpect(jsonPath("$.next.slug").value("1-2"));
    }

    @Test
    void withdrawnCourseAndLessonAreNotPublic() throws Exception {
        jdbc.update("UPDATE english_grammar_course SET publish_status='WITHDRAWN' WHERE id=1");
        mockMvc.perform(get("/api/v1/public/english/grammar")).andExpect(status().isNotFound());
        jdbc.update("UPDATE english_grammar_course SET publish_status='PUBLISHED' WHERE id=1");
        jdbc.update("UPDATE english_grammar_lesson SET publish_status='WITHDRAWN' WHERE slug='1-1'");
        mockMvc.perform(get("/api/v1/public/english/grammar/lessons/1-1")).andExpect(status().isNotFound());
    }

    @Test
    void grammarAdminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english/grammar/curriculum"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void explicitReassignWorksButCrossSectionEditIsRejected() throws Exception {
        Auth auth = login();
        long first = createSection(auth, "测试章节 A");
        long second = createSection(auth, "测试章节 B");
        long lesson = createLesson(auth, first);

        mockMvc.perform(withCsrf(put("/api/v1/admin/english/grammar/lessons/" + lesson)
                        .session(auth.session()).contentType("application/json")
                        .content("{\"sectionId\":" + second + ",\"title\":\"跨组\",\"slug\":\"99-1\",\"bodyMarkdown\":\"## 内容\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GRAMMAR_CROSS_SECTION_EDIT_FORBIDDEN"));

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/grammar/lessons/" + lesson + "/reassign")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"targetSectionId\":" + second + "}"), auth.csrf()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT section_id FROM english_grammar_lesson WHERE id=?", Long.class, lesson))
                .isEqualTo(second);

        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/grammar/sections/" + second)
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GRAMMAR_SECTION_NOT_EMPTY"));
    }

    @Test
    void grammarSearchHasDedicatedTypeAndCount() throws Exception {
        mockMvc.perform(get("/api/v1/public/search").param("q", "语法规则").param("type", "grammar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.grammar").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].type").value("GRAMMAR"))
                .andExpect(jsonPath("$.items[0].slug").value("1-1"));
    }

    private Auth login() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"grammar-admin\",\"password\":\"grammar-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return new Auth(session, csrfResult.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    private long createSection(Auth auth, String title) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/grammar/sections")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"title\":\"" + title + "\"}"), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createLesson(Auth auth, long sectionId) throws Exception {
        String body = "{\"sectionId\":" + sectionId + ",\"title\":\"测试课节\",\"slug\":\"99-1\"," +
                "\"summary\":\"测试\",\"bodyMarkdown\":\"## 学习内容\\n\\n- 测试\"}";
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/grammar/lessons")
                        .session(auth.session()).contentType("application/json").content(body), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private record Auth(MockHttpSession session, String csrf) { }
}

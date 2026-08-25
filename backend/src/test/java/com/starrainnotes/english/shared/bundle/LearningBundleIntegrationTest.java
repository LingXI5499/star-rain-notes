package com.starrainnotes.english.shared.bundle;

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

class LearningBundleIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('shared-admin',?)",
                passwordEncoder.encode("shared-pass-123"));
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_learning_bundle WHERE slug LIKE 'test-bundle-%'");
        jdbc.update("DELETE FROM english_reading_article WHERE slug='test-bundle-reading'");
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void createPublishAndServePublicly() throws Exception {
        Auth auth = login();
        long id = createBundle(auth, "test-bundle-pub", "test-bundle-pub", "A1");

        // draft not public yet
        mockMvc.perform(get("/api/v1/public/english/bundles/test-bundle-pub"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENGLISH_CONTENT_NOT_PUBLISHED"));

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles/" + id + "/publish")
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/public/english/bundles/test-bundle-pub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("test-bundle-pub"))
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
    }

    @Test
    void withdrawDraftIsRejected() throws Exception {
        Auth auth = login();
        long id = createBundle(auth, "test-bundle-w", "test-bundle-w", null);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles/" + id + "/withdraw")
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_INVALID_PUBLISH_TRANSITION"));
    }

    @Test
    void invalidCefrIsRejected() throws Exception {
        Auth auth = login();
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"title\":\"bad\",\"slug\":\"test-bundle-bad\",\"primaryCefr\":\"X9\"}"),
                        auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_CEFR_INVALID"));
    }

    @Test
    void duplicateSlugIsConflict() throws Exception {
        Auth auth = login();
        createBundle(auth, "test-bundle-dup", "test-bundle-dup", null);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"title\":\"dup\",\"slug\":\"test-bundle-dup\"}"), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_CONTENT_SLUG_CONFLICT"));
    }

    @Test
    void publicListOnlyReturnsPublished() throws Exception {
        Auth auth = login();
        long id = createBundle(auth, "test-bundle-live", "test-bundle-live", null);
        long hidden = createBundle(auth, "test-bundle-hidden", "test-bundle-hidden", null);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles/" + id + "/publish")
                .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/bundles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='test-bundle-live')].publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$[?(@.slug=='test-bundle-hidden')]").isEmpty());
    }

    @Test
    void bundleItemsSupportAddMoveAndPublishedFiltering() throws Exception {
        Auth auth=login();
        long bundle=createBundle(auth,"test-bundle-items","test-bundle-items","B1");
        jdbc.update("""
          INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
          publish_status,sort_order,published_at)
          VALUES ('Bundle reading','test-bundle-reading','summary','body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
          """);
        Long reading=jdbc.queryForObject("SELECT id FROM english_reading_article WHERE slug='test-bundle-reading'",Long.class);
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles/"+bundle+"/items")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"contentType\":\"READING\",\"contentId\":"+reading+"}"),auth.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("READING"));
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles/"+bundle+"/publish")
                .session(auth.session()),auth.csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/english/bundles/test-bundle-items/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("test-bundle-reading"));
        jdbc.update("UPDATE english_reading_article SET publish_status='WITHDRAWN' WHERE id=?",reading);
        mockMvc.perform(get("/api/v1/public/english/bundles/test-bundle-items/items"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private long createBundle(Auth auth, String title, String slug, String cefr) throws Exception {
        String cefrJson = cefr == null ? "null" : "\"" + cefr + "\"";
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/bundles")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"title\":\"" + title + "\",\"slug\":\"" + slug
                                + "\",\"primaryCefr\":" + cefrJson + "}"), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private Auth login() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"shared-admin\",\"password\":\"shared-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return new Auth(session, csrfResult.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    private record Auth(MockHttpSession session, String csrf) { }
}

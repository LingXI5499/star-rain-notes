package com.starrainnotes.english.shared;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnglishMetaAndTaxonomyIntegrationTest extends AbstractAuthIntegrationTest {

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
        jdbc.update("DELETE FROM english_taxonomy_term WHERE slug LIKE 'test-%' AND parent_id IS NOT NULL");
        jdbc.update("DELETE FROM english_taxonomy_term WHERE slug LIKE 'test-%'");
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void metaExposesTaxonomyCefrAndQuestionTypes() throws Exception {
        mockMvc.perform(get("/api/v1/public/english/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cefr.length()").value(6))
                .andExpect(jsonPath("$.cefr[0].level").value("A1"))
                .andExpect(jsonPath("$.questionTypes.READING").isArray())
                .andExpect(jsonPath("$.questionTypes.LISTENING").isArray())
                .andExpect(jsonPath("$.questionTypes.WRITING").isArray())
                .andExpect(jsonPath("$.taxonomy[?(@.slug=='topic-politics')].children.length()").value(3));
    }

    @Test
    void taxonomyAdminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/english/taxonomy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void seedCountsMatchDesign() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_taxonomy_term", Integer.class)).isEqualTo(49);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_taxonomy_term WHERE dimension='TOPIC' AND parent_id IS NULL",
                Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard", Integer.class)).isEqualTo(6);
    }

    @Test
    void createRejectsThirdLevel() throws Exception {
        Auth auth = login();
        long root = createTerm(auth, "TOPIC", null, "test-root", "test-root");
        long child = createTerm(auth, "TOPIC", root, "test-child", "test-child");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/taxonomy")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"dimension\":\"TOPIC\",\"parentId\":" + child
                                + ",\"name\":\"test-grandchild\",\"slug\":\"test-grandchild\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_DEPTH_INVALID"));
    }

    @Test
    void createRejectsCrossDimensionParent() throws Exception {
        Auth auth = login();
        long root = createTerm(auth, "TOPIC", null, "test-root", "test-root");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/taxonomy")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"dimension\":\"FUNCTION\",\"parentId\":" + root
                                + ",\"name\":\"test-fn\",\"slug\":\"test-fn\"}"), auth.csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_DEPTH_INVALID"));
    }

    @Test
    void deleteParentWithChildrenReturns409() throws Exception {
        Auth auth = login();
        long parentId = slugId("topic-politics");
        assertThat(parentId).isPositive();
        mockMvc.perform(withCsrf(delete("/api/v1/admin/english/taxonomy/" + parentId)
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENGLISH_TAXONOMY_IN_USE"));
    }

    @Test
    void moveNormalizesSiblingOrderToTens() throws Exception {
        Auth auth = login();
        long root = createTerm(auth, "TOPIC", null, "test-root", "test-root");
        long a = createTerm(auth, "TOPIC", root, "test-a", "test-a");
        long b = createTerm(auth, "TOPIC", root, "test-b", "test-b");
        long c = createTerm(auth, "TOPIC", root, "test-c", "test-c");
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/taxonomy/" + a + "/move")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"targetIndex\":2}"), auth.csrf()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT sort_order FROM english_taxonomy_term WHERE id=?",
                Integer.class, a)).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT sort_order FROM english_taxonomy_term WHERE id=?",
                Integer.class, b)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT sort_order FROM english_taxonomy_term WHERE id=?",
                Integer.class, c)).isEqualTo(20);
    }

    private long createTerm(Auth auth, String dimension, Long parentId, String name, String slug) throws Exception {
        String parent = parentId == null ? "null" : Long.toString(parentId);
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/taxonomy")
                        .session(auth.session()).contentType("application/json")
                        .content("{\"dimension\":\"" + dimension + "\",\"parentId\":" + parent
                                + ",\"name\":\"" + name + "\",\"slug\":\"" + slug + "\"}"), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long slugId(String slug) {
        Long id = jdbc.queryForObject(
                "SELECT id FROM english_taxonomy_term WHERE slug=?", Long.class, slug);
        return id == null ? -1 : id;
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

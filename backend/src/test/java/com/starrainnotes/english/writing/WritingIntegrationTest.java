package com.starrainnotes.english.writing;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WritingIntegrationTest extends AbstractAuthIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach void seedAdmin() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('writing-admin',?)",
                passwordEncoder.encode("writing-pass-123"));
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM english_writing_prompt");
        jdbc.update("DELETE FROM english_writing_resource");
        jdbc.update("DELETE FROM media_asset WHERE original_name='writing-cover.png'");
        jdbc.update("DELETE FROM admin_user");
    }
    @Test void adminResourceListReturnsTheNormalEmptyPage() throws Exception {
        Auth auth = login();
        mockMvc.perform(get("/api/v1/admin/english/writing/resources?page=1&pageSize=50")
                        .session(auth.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test void promptPreservesTagsReferencesAndCoverAndCanPublish() throws Exception {
        Auth auth = login();
        long coverId = uploadImage(auth);
        long templateId = createResource(auth, "TEMPLATE", "Writing template",
                "{\"version\":1,\"blocks\":[{\"id\":\"opening\",\"type\":\"textarea\"}]}");
        long modelId = createResource(auth, "MODEL_ESSAY", "Writing model", null);

        String payload = """
                {"title":"A real writing task","summary":"Practice a complete response",
                "backgroundMarkdown":"You are writing to a project partner.",
                "requirementsMarkdown":"Explain the problem and propose a solution.",
                "cefrLevel":"B1","wordMin":120,"wordMax":200,"estimatedMinutes":30,
                "rubricJson":"[{\\"name\\":\\"Content\\",\\"maxScore\\":40}]",
                "checklistJson":"[\\"I checked the task requirements.\\"]",
                "templateResourceId":%d,"modelResourceId":%d,"coverMediaId":%d,
                "tagIds":[1,18]}
                """.formatted(templateId, modelId, coverId);
        MvcResult created = mockMvc.perform(withCsrf(post("/api/v1/admin/english/writing/prompts")
                        .session(auth.session()).contentType("application/json").content(payload), auth.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverMediaId").value(coverId))
                .andExpect(jsonPath("$.templateResourceId").value(templateId))
                .andExpect(jsonPath("$.modelResourceId").value(modelId))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andReturn();
        long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(withCsrf(post("/api/v1/admin/english/writing/prompts/" + promptId + "/publish")
                        .session(auth.session()), auth.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));
    }

    @Test void globalSearchReturnsPublishedWritingResource() throws Exception {
        jdbc.update("""
                INSERT INTO english_writing_resource
                (resource_kind,title,slug,summary,body_markdown,cefr_level,estimated_minutes,publish_status,sort_order,published_at)
                VALUES ('GENRE_LESSON','Writing search guide','writing-search-guide','Searchable writing summary',
                '## Search writing practice','B1',5,'PUBLISHED',10,UTC_TIMESTAMP(6))
                """);
        mockMvc.perform(get("/api/v1/public/search?q=writing&type=writing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.writing").value(1))
                .andExpect(jsonPath("$.items[0].type").value("WRITING"))
                .andExpect(jsonPath("$.items[0].tutorialSlug").value("resource"));
    }

    private long createResource(Auth auth, String kind, String title, String templateSchema) throws Exception {
        String schema = templateSchema == null ? "null" : objectMapper.writeValueAsString(templateSchema);
        String body = "{\"resourceKind\":\"" + kind + "\",\"title\":\"" + title
                + "\",\"summary\":\"A complete learning resource\",\"bodyMarkdown\":\"## Body\","
                + "\"cefrLevel\":\"B1\",\"wordMin\":0,\"wordMax\":300,\"estimatedMinutes\":10,"
                + "\"templateSchemaJson\":" + schema + ",\"tagIds\":[1]}";
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/admin/english/writing/resources")
                        .session(auth.session()).contentType("application/json").content(body), auth.csrf()))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(withCsrf(post("/api/v1/admin/english/writing/resources/" + id + "/publish")
                        .session(auth.session()), auth.csrf())).andExpect(status().isOk());
        return id;
    }

    private long uploadImage(Auth auth) throws Exception {
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        MvcResult result = mockMvc.perform(withCsrf(multipart("/api/v1/admin/media-assets")
                        .file(new MockMultipartFile("file", "writing-cover.png", "image/png", png)), auth.csrf())
                        .session(auth.session()))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Auth login() throws Exception {
        var result = mockMvc.perform(withCsrf(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"writing-admin\",\"password\":\"writing-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return new Auth(session, csrf.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    private record Auth(MockHttpSession session, String csrf) { }
}

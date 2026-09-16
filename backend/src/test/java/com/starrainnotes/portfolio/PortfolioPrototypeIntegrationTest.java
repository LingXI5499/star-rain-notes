package com.starrainnotes.portfolio;

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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Static prototype upload, media-library binding and ONLINE validation.
 */
class PortfolioPrototypeIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedAdmin() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("DELETE FROM portfolio_project_prototype");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("DELETE FROM portfolio_project_prototype");
        jdbc.update("DELETE FROM portfolio_project");
        jdbc.update("DELETE FROM media_asset");
    }

    @Test
    void uploadsAndReplacesAValidatedPrototype() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "prototype-upload");
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.zip", "application/zip", zip("index.html", "<h1>Demo</h1>", "assets/app.js", "document.body.dataset.ready='1'"));
        mockMvc.perform(withCsrf(multipart("/api/v1/admin/portfolio/projects/" + projectId + "/prototype").file(file), fetchCsrfToken())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceName").value("demo.zip"))
                .andExpect(jsonPath("$.fileCount").value(2))
                .andExpect(jsonPath("$.previewUrl").isNotEmpty());

        mockMvc.perform(withCsrf(put("/api/v1/admin/portfolio/projects/" + projectId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Prototype\",\"slug\":\"prototype-upload\",\"summary\":\"S\","
                                + "\"bodyMarkdown\":\"B\",\"projectStatus\":\"ONLINE\"}"), fetchCsrfToken()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectStatus").value("ONLINE"));
    }

    @Test
    void uploadsArchiveToMediaLibraryThenBindsItToAProject() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "prototype-media");
        MockMultipartFile file = new MockMultipartFile(
                "file", "library.zip", "application/zip", zip("index.html", "<h1>Library</h1>"));
        MvcResult uploaded = mockMvc.perform(withCsrf(multipart("/api/v1/admin/media-assets").file(file), fetchCsrfToken())
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType").value("ARCHIVE"))
                .andReturn();
        long mediaId = objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(withCsrf(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/admin/portfolio/projects/" + projectId + "/prototype/media/" + mediaId), fetchCsrfToken())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaAssetId").value(mediaId))
                .andExpect(jsonPath("$.sourceName").value("library.zip"));
    }

    @Test
    void acceptsACommonSingleProjectFolderWrapper() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "prototype-wrapper");
        MockMultipartFile file = new MockMultipartFile(
                "file", "wrapped.zip", "application/x-zip-compressed",
                zip("five-page-site/index.html", "<h1>Wrapped</h1>",
                        "five-page-site/assets/app.js", "document.body.dataset.ready='1'"));
        mockMvc.perform(withCsrf(multipart("/api/v1/admin/portfolio/projects/" + projectId + "/prototype").file(file), fetchCsrfToken())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileCount").value(2))
                .andExpect(jsonPath("$.previewUrl").value(org.hamcrest.Matchers.endsWith("/five-page-site/index.html")));
    }

    @Test
    void rejectsArchiveWithMultipleTopLevelEntryPages() throws Exception {
        MockHttpSession session = loginSession();
        Long projectId = createProject(session, "prototype-ambiguous");
        MockMultipartFile file = new MockMultipartFile(
                "file", "ambiguous.zip", "application/zip",
                zip("site-a/index.html", "<h1>A</h1>", "site-b/index.html", "<h1>B</h1>"));
        mockMvc.perform(withCsrf(multipart("/api/v1/admin/portfolio/projects/" + projectId + "/prototype").file(file), fetchCsrfToken())
                        .session(session))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PROTOTYPE_ARCHIVE"));
    }

    private Long createProject(MockHttpSession session, String slug) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/admin/portfolio/projects",
                        "{\"title\":\"Prototype\",\"slug\":\"" + slug + "\",\"summary\":\"S\",\"bodyMarkdown\":\"B\"}"),
                        fetchCsrfToken()).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static byte[] zip(String... entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int i = 0; i < entries.length; i += 2) {
                zip.putNextEntry(new ZipEntry(entries[i]));
                zip.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private MockHttpSession loginSession() throws Exception {
        String csrf = fetchCsrfToken();
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"), csrf))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

package com.starrainnotes.english.writing;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WritingIntegrationTest extends AbstractAuthIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach void seedAdmin() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('writing-admin',?)",
                passwordEncoder.encode("writing-pass-123"));
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM english_writing_prompt");
        jdbc.update("DELETE FROM english_writing_resource");
        jdbc.update("DELETE FROM admin_user");
    }
    @Test void adminResourceListReturnsTheNormalEmptyPage() throws Exception {
        MockHttpSession session = login();
        mockMvc.perform(get("/api/v1/admin/english/writing/resources?page=1&pageSize=50")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(0));
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

    private MockHttpSession login() throws Exception {
        var result = mockMvc.perform(withCsrf(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"writing-admin\",\"password\":\"writing-pass-123\"}"), fetchCsrfToken()))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

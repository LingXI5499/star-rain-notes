package com.starrainnotes.seo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seo.site-origin=https://example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeoIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void homeContainsRawSemanticSeo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<h1>")))
                .andExpect(content().string(containsString("rel=\"canonical\" href=\"https://example.com/\"")))
                .andExpect(content().string(containsString("application/ld+json")));
    }

    @Test
    void missingDynamicDocumentIsReal404AndNoIndex() throws Exception {
        mockMvc.perform(get("/blog/this-page-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(content().string(containsString("页面不存在")));
    }

    @Test
    void sitemapAndRobotsExcludePrivateRoutes() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://example.com/")))
                .andExpect(content().string(not(containsString("/admin"))))
                .andExpect(content().string(not(containsString("/search"))));
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap: https://example.com/sitemap.xml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/tutorials", "/blog", "/portfolio", "/english", "/english/grammar",
            "/english/reading", "/english/listening", "/english/listening/pronunciation",
            "/english/writing", "/english/bundles", "/about", "/search"})
    void publicEntryPagesRenderFromTheDatabase(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string(containsString("<h1>")));
    }
}

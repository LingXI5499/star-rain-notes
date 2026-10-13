package com.starrainnotes.seo;

import com.starrainnotes.seo.controller.SeoHtmlController;
import com.starrainnotes.seo.service.SeoHtmlPageService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTML route status is an HTTP contract, independent of the browser's SPA fallback. */
class SeoHtmlHttpContractTest {
    /** Missing content must be 404 on each public detail path. */
    @ParameterizedTest
    @ValueSource(strings = {"/blog/posts/missing", "/tutorials/course/missing", "/portfolio/missing"})
    void absentSnapshotIs404(String path) throws Exception {
        SeoHtmlPageService service = mock(SeoHtmlPageService.class);
        when(service.html(path)).thenReturn(Optional.empty());
        MockMvcBuilders.standaloneSetup(new SeoHtmlController(service)).build()
                .perform(get(path).accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound());
        verify(service).html(path);
    }

    /** A stored public snapshot is delivered as HTML, without a second generated shell. */
    @Test void publicSnapshotIsHtml200() throws Exception {
        SeoHtmlPageService service = mock(SeoHtmlPageService.class);
        String html = "<!doctype html><html><head><title>Fixture</title></head><body><main>Content</main></body></html>";
        when(service.html("/blog/posts/fixture")).thenReturn(Optional.of(html));
        MockMvcBuilders.standaloneSetup(new SeoHtmlController(service)).build()
                .perform(get("/blog/posts/fixture").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(html));
    }
}

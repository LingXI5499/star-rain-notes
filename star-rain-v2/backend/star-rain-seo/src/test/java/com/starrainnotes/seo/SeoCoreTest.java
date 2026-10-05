package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.seo.service.CanonicalService;
import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.service.SeoHtmlRenderer;
import com.starrainnotes.seo.dto.SeoPageModel;
import com.starrainnotes.seo.service.SitemapService;
import com.starrainnotes.seo.service.impl.SitemapServiceImpl;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeoCoreTest {
    @Test
    void canonicalUsesConfiguredOriginAndRejectsUnsafeRoute() {
        SeoProperties config = new SeoProperties();
        config.setPublicBaseUrl("https://example.org/");
        CanonicalService service = new CanonicalService(config);
        assertEquals("https://example.org/blog/posts/hello", service.canonical("/blog/posts/hello"));
        assertThrows(RuntimeException.class, () -> service.canonical("//attacker.test/blog"));
        assertThrows(RuntimeException.class, () -> service.canonical("/blog?host=attacker.test"));
    }

    @Test
    void htmlEscapesUntrustedContentAndKeepsCoreText() {
        String html = new SeoHtmlRenderer().render(SeoPageModel.builder()
            .canonicalUrl("https://example.org/blog/posts/test")
            .title("<script>alert(1)</script>")
            .description("Java & Vue")
            .robotsDirective("index,follow")
            .bodyMarkdown("# 排序算法\n<img src=x onerror=alert(1)>\n**稳定排序**")
            .build());
        assertTrue(html.contains("排序算法"));
        assertTrue(html.contains("稳定排序"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertFalse(html.contains("<img src=x"));
    }

    @Test
    void sitemapEscapesCanonicalUrls() {
        SeoPageMapper mapper = mock(SeoPageMapper.class);
        SeoPageSnapshot page = new SeoPageSnapshot();
        page.setCanonicalUrl("https://example.org/blog?a=1&b=2");
        when(mapper.activePages()).thenReturn(List.of(page));
        String xml = new SitemapServiceImpl(mapper).xml();
        assertTrue(xml.contains("a=1&amp;b=2"));
        assertEquals(1, xml.split("<url>", -1).length - 1);
    }
}

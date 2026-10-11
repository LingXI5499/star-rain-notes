package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.seo.dto.SeoPageModel;
import com.starrainnotes.seo.renderer.SeoHtmlRenderer;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.service.impl.SitemapServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeoSemanticRenderingTest {
    private final SeoHtmlRenderer renderer = new SeoHtmlRenderer();
    private String render(String body) {
        return renderer.render(SeoPageModel.builder().title("Title").description("Description")
                .canonicalUrl("https://example.org/blog/posts/test").robotsDirective("index,follow")
                .contentType("BLOG").bodyMarkdown(body).build());
    }

    @Test void preservesSemanticMarkdownAndOneMainHeading() {
        String html = render("""
                # Title
                ## Collections
                ### ArrayList

                - Ordered
                - Random access

                [Documentation](https://docs.oracle.com/)

                ```java
                List<String> names = new ArrayList<>();
                ```

                > **Note** and *emphasis*: $x^2 + y^2$

                | Name | Value |
                | --- | --- |
                | Item | 1 |
                """);
        for (String tag : List.of("<h2>", "<h3>", "<ul>", "<li>", "<pre>", "<code", "<blockquote>", "<strong>", "<em>", "<table>")) assertTrue(html.contains(tag), tag);
        assertTrue(html.contains("href=\"https://docs.oracle.com/\""));
        assertTrue(html.contains("$x^2 + y^2$"));
        assertTrue(html.contains("List&lt;String&gt;"));
        assertEquals(1, html.split("<h1>", -1).length - 1);
        assertTrue(html.contains("\"@type\":\"BlogPosting\""));
    }

    @Test void preventsRawHtmlAndUnsafeUrlExecution() {
        String html = render("""
                <script>alert(1)</script>
                <iframe src="https://example.org"></iframe>
                <img src=x onerror=alert(1)>

                [Bad](javascript:alert%281%29)
                ![Bad](data:image/svg+xml,evil)
                [Protocol relative](//attacker.example)
                ![Safe](/api/public/media/1/content)
                """);
        assertFalse(html.contains("<script>alert"));
        assertFalse(html.contains("<iframe"));
        assertFalse(html.contains("<img src=x"));
        assertFalse(html.contains("href=\"javascript:"));
        assertFalse(html.contains("src=\"data:"));
        assertFalse(html.contains("href=\"//attacker"));
        assertTrue(html.contains("src=\"/api/public/media/1/content\""));
    }

    @Test void keepsMetadataAndJsonLdSafeAndBoundsLargeContent() {
        String html = renderer.render(SeoPageModel.builder().title("</script><script>alert(1)</script>")
                .description("\" & <>").bodyMarkdown("a".repeat(210_000)).build());
        assertFalse(html.contains("</script><script>alert"));
        assertTrue(html.contains("\\u003c/script\\u003e"));
        assertTrue(html.contains("&quot; &amp; &lt;&gt;"));
        assertTrue(html.length() < 205_000);
    }

    @Test void sitemapUsesSourceDateAndOmitsMissingOrOpaqueDates() {
        SeoPageMapper mapper = mock(SeoPageMapper.class);
        SeoPageSnapshot page = new SeoPageSnapshot();
        page.setCanonicalUrl("https://example.org/blog/test");
        page.setSourceVersionRef("2026-10-10T12:00:00");
        page.setGeneratedAt(LocalDateTime.of(2026, 10, 11, 12, 0));
        when(mapper.activePages()).thenReturn(List.of(page));
        var sitemap = new SitemapServiceImpl(mapper);
        String before = sitemap.xml();
        page.setGeneratedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
        assertEquals(before, sitemap.xml());
        assertTrue(before.contains("<lastmod>2026-10-10T12:00Z</lastmod>"));
        page.setSourceVersionRef("2026-10-12T12:00:00");
        assertTrue(sitemap.xml().contains("2026-10-12T12:00Z"));
        page.setSourceVersionRef("revision:4");
        assertFalse(sitemap.xml().contains("<lastmod>"));
        page.setSourceVersionRef(null);
        assertFalse(sitemap.xml().contains("<lastmod>"));
        when(mapper.activePages()).thenReturn(List.of());
        assertFalse(sitemap.xml().contains("<url>"));
    }
}

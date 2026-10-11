/*
 * SeoSemanticRenderingTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SeoPageModel、SeoHtmlRenderer、SeoPageSnapshot、SeoPageMapper、SitemapServiceImpl。
 * 阅读入口：render（渲染SEOSemanticRendering）；preservesSemanticMarkdownAndOneMainHeading（preservesSemanticMarkdown及单次MainHeading处理入口）；preventsRawHtmlAndUnsafeUrlExecution（验证防止原始HTML及不安全地址执行）；keepsMetadataAndJsonLdSafeAndBoundsLargeContent（验证保留元数据及JSONLd安全及BoundsLarge内容）。
 */

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

/**
 * SEOSemanticRendering测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoSemanticRenderingTest {
    /**
     * 渲染器。
     */
    private final SeoHtmlRenderer renderer = new SeoHtmlRenderer();
    /**
     * 渲染SEOSemanticRendering。
     * 主要协作调用：<code>renderer.render</code>。
     *
     * @param body 正文；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String render(String body) {
        return renderer.render(SeoPageModel.builder().title("Title").description("Description")
                .canonicalUrl("https://example.org/blog/posts/test").robotsDirective("index,follow")
                .contentType("BLOG").bodyMarkdown(body).build());
    }

    /**
     * preservesSemanticMarkdown及单次MainHeading处理入口。
     * 关键处理步骤调用 <code>render</code>、<code>List.of</code>、<code>html.contains</code>、<code>html.split</code>。
     */
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

    /**
     * 验证防止原始HTML及不安全地址执行。
     * 关键处理步骤调用 <code>render</code>、<code>html.contains</code>。
     */
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

    /**
     * 验证保留元数据及JSONLd安全及BoundsLarge内容。
     * 主要协作调用：<code>renderer.render</code>。
     */
    @Test void keepsMetadataAndJsonLdSafeAndBoundsLargeContent() {
        String html = renderer.render(SeoPageModel.builder().title("</script><script>alert(1)</script>")
                .description("\" & <>").bodyMarkdown("a".repeat(210_000)).build());
        assertFalse(html.contains("</script><script>alert"));
        assertTrue(html.contains("\\u003c/script\\u003e"));
        assertTrue(html.contains("&quot; &amp; &lt;&gt;"));
        assertTrue(html.length() < 205_000);
    }

    /**
     * sitemapUses来源日期及Omits缺失或OpaqueDates处理入口。
     * 关键处理步骤调用 <code>page.setCanonicalUrl</code>、<code>page.setSourceVersionRef</code>、<code>page.setGeneratedAt</code>、<code>LocalDateTime.of</code>。
     */
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

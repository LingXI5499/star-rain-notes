/*
 * SeoCoreTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CanonicalUrlResolver、SeoProperties、SeoPageMapper、SeoHtmlRenderer、SeoPageModel、SitemapService、SitemapServiceImpl。
 * 阅读入口：canonicalUsesConfiguredOriginAndRejectsUnsafeRoute（规范UsesConfigured来源及拒绝不安全路由处理入口）；robotsIsPlainTextWithConfiguredSitemap（robots是否纯文本文本包含ConfiguredSitemap处理入口）；htmlEscapesUntrustedContentAndKeepsCoreText（HTMLEscapesUntrusted内容及保留Core文本处理入口）；sitemapEscapesCanonicalUrls（sitemapEscapes规范Urls处理入口）。
 */

package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.seo.config.CanonicalUrlResolver;
import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.renderer.SeoHtmlRenderer;
import com.starrainnotes.seo.dto.SeoPageModel;
import com.starrainnotes.seo.service.SitemapService;
import com.starrainnotes.seo.service.impl.SitemapServiceImpl;
import com.starrainnotes.seo.service.impl.RobotsServiceImpl;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * SEOCore测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoCoreTest {
    @Test
    void optionalCoverDoesNotProduceNullOpenGraphOrStructuredImages() {
        String html = new SeoHtmlRenderer().render(SeoPageModel.builder()
            .canonicalUrl("https://example.org/portfolio/no-cover").contentType("PORTFOLIO")
            .title("无封面作品").description("作品简介").robotsDirective("index,follow")
            .bodyMarkdown("# 创作过程").coverUrl(null).build());
        assertTrue(html.contains("无封面作品"));
        assertTrue(html.contains("创作过程"));
        assertFalse(html.contains("og:image"));
        assertFalse(html.contains("\"image\":null"));
    }
    /**
     * 规范UsesConfigured来源及拒绝不安全路由处理入口。
     * 关键处理步骤调用 <code>config.setPublicBaseUrl</code>、<code>service.canonical</code>。
     */
    @Test
    void canonicalUsesConfiguredOriginAndRejectsUnsafeRoute() {
        SeoProperties config = new SeoProperties();
        config.setPublicBaseUrl("https://example.org/");
        CanonicalUrlResolver service = new CanonicalUrlResolver(config);
        assertEquals("https://example.org/blog/posts/hello", service.canonical("/blog/posts/hello"));
        assertThrows(RuntimeException.class, () -> service.canonical("//attacker.test/blog"));
        assertThrows(RuntimeException.class, () -> service.canonical("/blog?host=attacker.test"));
    }

    /**
     * robots是否纯文本文本包含ConfiguredSitemap处理入口。
     * 关键处理步骤调用 <code>config.setPublicBaseUrl</code>、<code>content.contains</code>、<code>content.endsWith</code>。
     */
    @Test
    void robotsIsPlainTextWithConfiguredSitemap() {
        SeoProperties config = new SeoProperties();
        config.setPublicBaseUrl("https://example.org/");
        String content = new RobotsServiceImpl(new CanonicalUrlResolver(config)).robotsTxt();
        assertTrue(content.contains("Disallow: /api/\n"));
        assertTrue(content.contains("Allow: /api/public/\n"));
        assertTrue(content.contains("Disallow: /useradmin/\n"));
        assertTrue(content.endsWith("Sitemap: https://example.org/sitemap.xml\n"));
    }

    /**
     * HTMLEscapesUntrusted内容及保留Core文本处理入口。
     * 关键处理步骤调用 <code>SeoPageModel.builder</code>、<code>html.contains</code>。
     */
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

    /**
     * sitemapEscapes规范Urls处理入口。
     * 关键处理步骤调用 <code>page.setCanonicalUrl</code>、<code>mapper.activePages</code>、<code>List.of</code>、<code>xml.contains</code>。
     */
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

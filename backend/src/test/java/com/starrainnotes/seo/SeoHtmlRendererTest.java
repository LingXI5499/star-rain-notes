package com.starrainnotes.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoHtmlRendererTest {
    @Test
    void rendersCanonicalMetadataSemanticBodyAndJsonLd() {
        SeoIdentityService identity = mock(SeoIdentityService.class);
        when(identity.site()).thenReturn(new SeoIdentityService.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        SeoProperties properties = new SeoProperties("https://yulanlin.cn", "", "/brand/og-default.png", false, "", false, "", "https://yulanlin.cn");
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(properties, identity, new ObjectMapper());
        SeoPage page = new SeoPage("/blog/123", "真实文章", "真实摘要", "index,follow", "article", "Article", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0),
                "<h2>正文</h2><p>内容</p>", List.of(new SeoBreadcrumb("博客", "/blog")), Map.of());

        String html = renderer.render(page);

        assertThat(html).contains("<title>真实文章 | 星雨笔录</title>");
        assertThat(html).contains("rel=\"canonical\" href=\"https://yulanlin.cn/blog/123\"");
        assertThat(html).contains("property=\"og:type\" content=\"article\"");
        assertThat(html).contains("application/ld+json", "BreadcrumbList", "<h1>真实文章</h1>", "<h2>正文</h2>");
    }

    @Test
    void notFoundIsNoIndex() {
        SeoIdentityService identity = mock(SeoIdentityService.class);
        when(identity.site()).thenReturn(new SeoIdentityService.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(new SeoProperties(null, null, null, false, null, false, null, null), identity, new ObjectMapper());
        assertThat(renderer.renderNotFound("/missing")).contains("noindex,nofollow", "页面不存在", "返回首页");
    }

    @Test
    void shellBrandAssetsPassThroughToCrawlerHtml() {
        SeoIdentityService identity = mock(SeoIdentityService.class);
        when(identity.site()).thenReturn(new SeoIdentityService.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(new SeoProperties("https://yulanlin.cn", "", null, false, null, false, null, null), identity, new ObjectMapper());
        String html = renderer.render(new SeoPage("/", "星雨笔录", "建立自己的知识世界", "index,follow", "website", "WebSite", null, null, null,
                "<p>首页</p>", List.of(), Map.of()));

        // Crawler HTML keeps the favicon / manifest brand links from the shell.
        assertThat(html).contains("rel=\"icon\"", "/brand/favicon.png");
        assertThat(html).doesNotContain("favicon.svg");
        assertThat(html).contains("rel=\"apple-touch-icon\"");
        assertThat(html).contains("rel=\"manifest\"");
        // and the default share image migrates to the platform-friendly format
        assertThat(html).contains("property=\"og:image\" content=\"https://yulanlin.cn/brand/og-default.png\"");
    }

    @Test
    void jsonLdEscapesScriptTerminatorsWithoutChangingStructuredData() throws Exception {
        String hostileTitle = "</script><script>alert(1)</script>";
        SeoIdentityService identity = mock(SeoIdentityService.class);
        when(identity.site()).thenReturn(new SeoIdentityService.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        ObjectMapper json = new ObjectMapper();
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(
                new SeoProperties("https://yulanlin.cn", "", null, false, null, false, null, null), identity, json);

        String html = renderer.render(new SeoPage("/blog/123", hostileTitle, "Summary", "index,follow",
                "article", "Article", null, null, null, "<p>Content</p>", List.of(), Map.of()));
        String marker = "<script type=\"application/ld+json\" data-seo-schema>\n";
        int start = html.indexOf(marker) + marker.length();
        int end = html.indexOf("\n</script>", start);

        assertThat(html).doesNotContain("</script><script>alert(1)");
        assertThat(html.substring(start, end)).contains("\\u003c/script\\u003e");
        assertThat(json.readTree(html.substring(start, end)).path("@graph").get(0).path("name").asText())
                .isEqualTo(hostileTitle);
    }
}

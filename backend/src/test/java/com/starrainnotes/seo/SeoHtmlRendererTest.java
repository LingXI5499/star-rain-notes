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
        SeoContentRepository content = mock(SeoContentRepository.class);
        when(content.site()).thenReturn(new SeoContentRepository.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        SeoProperties properties = new SeoProperties("https://yulanlin.cn", "", "/og-default.svg", false, "", false, "", "https://yulanlin.cn");
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(properties, content, new ObjectMapper());
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
        SeoContentRepository content = mock(SeoContentRepository.class);
        when(content.site()).thenReturn(new SeoContentRepository.SiteIdentity("星雨笔录", "建立自己的知识世界", null));
        SeoHtmlRenderer renderer = new SeoHtmlRenderer(new SeoProperties(null, null, null, false, null, false, null, null), content, new ObjectMapper());
        assertThat(renderer.renderNotFound("/missing")).contains("noindex,nofollow", "页面不存在", "返回首页");
    }
}

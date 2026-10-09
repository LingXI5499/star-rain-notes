package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.starrainnotes.blog.constant.BlogLimits;
import org.junit.jupiter.api.Test;

class BlogPostSlugDeriverTest {

    @Test
    void derivesAsciiTitle() {
        assertThat(BlogPostSlugDeriver.derive("Hello World 2026")).isEqualTo("hello-world-2026");
    }

    @Test
    void fallsBackForChineseTitle() {
        String slug = BlogPostSlugDeriver.derive("纯中文标题");
        assertThat(slug).startsWith("post-");
        assertThat(BlogSlugRules.isValid(slug, BlogLimits.POST_SLUG_MAX_LENGTH)).isTrue();
    }

    @Test
    void keepsAsciiPartOfMixedTitle() {
        assertThat(BlogPostSlugDeriver.derive("Java 开发指南")).isEqualTo("java");
    }

    @Test
    void limitsLongTitleAndReservesSuffixSpace() {
        String base = BlogPostSlugDeriver.derive("Long Article ".repeat(25));
        assertThat(base.length()).isLessThanOrEqualTo(BlogLimits.POST_SLUG_MAX_LENGTH);
        assertThat(BlogSlugRules.isValid(BlogPostSlugDeriver.withSuffix(base, 1000),
                BlogLimits.POST_SLUG_MAX_LENGTH)).isTrue();
    }

    @Test
    void fallsBackForSymbolsAndVeryShortAscii() {
        assertThat(BlogPostSlugDeriver.derive("?!…")).startsWith("post-");
        assertThat(BlogPostSlugDeriver.derive("A")).startsWith("post-");
    }

    @Test
    void repeatedTitleAlwaysHasSameBase() {
        assertThat(BlogPostSlugDeriver.derive("纯中文标题"))
                .isEqualTo(BlogPostSlugDeriver.derive("纯中文标题"));
        assertThat(BlogPostSlugDeriver.withSuffix(BlogPostSlugDeriver.derive("纯中文标题"), 2))
                .endsWith("-2");
    }
}

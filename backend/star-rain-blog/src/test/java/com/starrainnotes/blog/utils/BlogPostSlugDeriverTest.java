/*
 * BlogPostSlugDeriverTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogLimits。
 * 阅读入口：derivesAsciiTitle（derivesAscii标题处理入口）；fallsBackForChineseTitle（fallsBack面向Chinese标题处理入口）；keepsAsciiPartOfMixedTitle（验证保留AsciiPart所属Mixed标题）；limitsLongTitleAndReservesSuffixSpace（limitsLong标题及ReservesSuffixSpace处理入口）。
 */

package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.starrainnotes.blog.constant.BlogLimits;
import org.junit.jupiter.api.Test;

/**
 * 博客文章固定链接标识Deriver测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogPostSlugDeriverTest {

    /**
     * derivesAscii标题处理入口。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>。
     */
    @Test
    void derivesAsciiTitle() {
        assertThat(BlogPostSlugDeriver.derive("Hello World 2026")).isEqualTo("hello-world-2026");
    }

    /**
     * fallsBack面向Chinese标题处理入口。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>、<code>BlogSlugRules.isValid</code>。
     */
    @Test
    void fallsBackForChineseTitle() {
        String slug = BlogPostSlugDeriver.derive("纯中文标题");
        assertThat(slug).startsWith("post-");
        assertThat(BlogSlugRules.isValid(slug, BlogLimits.POST_SLUG_MAX_LENGTH)).isTrue();
    }

    /**
     * 验证保留AsciiPart所属Mixed标题。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>。
     */
    @Test
    void keepsAsciiPartOfMixedTitle() {
        assertThat(BlogPostSlugDeriver.derive("Java 开发指南")).isEqualTo("java");
    }

    /**
     * limitsLong标题及ReservesSuffixSpace处理入口。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>、<code>base.length</code>、<code>BlogSlugRules.isValid</code>、<code>BlogPostSlugDeriver.withSuffix</code>。
     */
    @Test
    void limitsLongTitleAndReservesSuffixSpace() {
        String base = BlogPostSlugDeriver.derive("Long Article ".repeat(25));
        assertThat(base.length()).isLessThanOrEqualTo(BlogLimits.POST_SLUG_MAX_LENGTH);
        assertThat(BlogSlugRules.isValid(BlogPostSlugDeriver.withSuffix(base, 1000),
                BlogLimits.POST_SLUG_MAX_LENGTH)).isTrue();
    }

    /**
     * fallsBack面向Symbols及VeryShortAscii处理入口。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>。
     */
    @Test
    void fallsBackForSymbolsAndVeryShortAscii() {
        assertThat(BlogPostSlugDeriver.derive("?!…")).startsWith("post-");
        assertThat(BlogPostSlugDeriver.derive("A")).startsWith("post-");
    }

    /**
     * repeated标题Always是否具备Same基础处理入口。
     * 关键处理步骤调用 <code>BlogPostSlugDeriver.derive</code>、<code>BlogPostSlugDeriver.withSuffix</code>。
     */
    @Test
    void repeatedTitleAlwaysHasSameBase() {
        assertThat(BlogPostSlugDeriver.derive("纯中文标题"))
                .isEqualTo(BlogPostSlugDeriver.derive("纯中文标题"));
        assertThat(BlogPostSlugDeriver.withSuffix(BlogPostSlugDeriver.derive("纯中文标题"), 2))
                .endsWith("-2");
    }
}

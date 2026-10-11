/*
 * BlogSlugDeriverTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：taxonomyNamesGenerateReadableAndStableSlugs（taxonomyNamesGenerateReadable及StableSlugs处理入口）；collisionSuffixFitsLimit（collisionSuffixFits上限处理入口）。
 */

package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 博客固定链接标识Deriver测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogSlugDeriverTest {

    /**
     * taxonomyNamesGenerateReadable及StableSlugs处理入口。
     * 关键处理步骤调用 <code>BlogSlugDeriver.derive</code>。
     */
    @Test
    void taxonomyNamesGenerateReadableAndStableSlugs() {
        assertThat(BlogSlugDeriver.derive("Spring Boot", "tag", 100)).isEqualTo("spring-boot");
        String chinese = BlogSlugDeriver.derive("学习路线", "topic", 120);
        assertThat(chinese).matches("topic-[a-f0-9]{12}");
        assertThat(BlogSlugDeriver.derive("学习路线", "topic", 120)).isEqualTo(chinese);
    }

    /**
     * collisionSuffixFits上限处理入口。
     * 关键处理步骤调用 <code>BlogSlugDeriver.derive</code>、<code>BlogSlugDeriver.withSuffix</code>。
     */
    @Test
    void collisionSuffixFitsLimit() {
        String base = BlogSlugDeriver.derive("a".repeat(150), "tag", 100);
        assertThat(BlogSlugDeriver.withSuffix(base, 2, 100)).hasSize(100).endsWith("-2");
    }
}

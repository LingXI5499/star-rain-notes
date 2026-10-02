package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlogSlugDeriverTest {

    @Test
    void taxonomyNamesGenerateReadableAndStableSlugs() {
        assertThat(BlogSlugDeriver.derive("Spring Boot", "tag", 100)).isEqualTo("spring-boot");
        String chinese = BlogSlugDeriver.derive("学习路线", "topic", 120);
        assertThat(chinese).matches("topic-[a-f0-9]{12}");
        assertThat(BlogSlugDeriver.derive("学习路线", "topic", 120)).isEqualTo(chinese);
    }

    @Test
    void collisionSuffixFitsLimit() {
        String base = BlogSlugDeriver.derive("a".repeat(150), "tag", 100);
        assertThat(BlogSlugDeriver.withSuffix(base, 2, 100)).hasSize(100).endsWith("-2");
    }
}

package com.starrainnotes.seo.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import com.starrainnotes.seo.provider.impl.BlogTopicSeoSourceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlogTopicSeoSourceProviderTest {
    private final BlogPublicApi blogs = mock(BlogPublicApi.class);
    private final BlogTopicSeoSourceProvider provider = new BlogTopicSeoSourceProvider(blogs);

    @Test
    void suppliesPublicTopicMetadataAndSitemapDocuments() {
        BlogTopicSummary topic = new BlogTopicSummary(7L, "java", "Java", "Java 专题简介", null);
        when(blogs.publishedTopicBySlug("java")).thenReturn(topic);
        when(blogs.publishedTopics()).thenReturn(List.of(topic));

        var document = provider.loadByRoute("/blog/topics/java").orElseThrow();
        assertThat(document.getTitle()).isEqualTo("Java");
        assertThat(document.getSummary()).isEqualTo("Java 专题简介");
        assertThat(document.getRoutePath()).isEqualTo("/blog/topics/java");
        assertThat(provider.listPublished()).containsExactly(document);
    }

    @Test
    void excludesDisabledOrUnknownTopics() {
        when(blogs.publishedTopicBySlug("hidden")).thenReturn(null);
        assertThat(provider.loadByRoute("/blog/topics/hidden")).isEmpty();
        assertThat(provider.supports("/blog/topics/../../hidden")).isFalse();
    }
}

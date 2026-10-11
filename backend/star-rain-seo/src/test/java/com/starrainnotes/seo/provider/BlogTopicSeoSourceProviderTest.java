/*
 * BlogTopicSeoSourceProviderTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogPublicApi、BlogTopicSummary、BlogTopicSeoSourceProvider。
 * 阅读入口：suppliesPublicTopicMetadataAndSitemapDocuments（supplies公开专题元数据及SitemapDocuments处理入口）；excludesDisabledOrUnknownTopics（excludes已禁用或Unknown专题列表处理入口）。
 */

package com.starrainnotes.seo.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import com.starrainnotes.seo.provider.impl.BlogTopicSeoSourceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 博客专题SEO来源提供方测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogTopicSeoSourceProviderTest {
    /**
     * blogs。
     * 协作类型：<code>BlogPublicApi</code>。
     */
    private final BlogPublicApi blogs = mock(BlogPublicApi.class);
    /**
     * 提供方。
     */
    private final BlogTopicSeoSourceProvider provider = new BlogTopicSeoSourceProvider(blogs);

    /**
     * supplies公开专题元数据及SitemapDocuments处理入口。
     * 主要协作调用：<code>blogs.publishedTopicBySlug</code>、<code>blogs.publishedTopics</code>、<code>provider.loadByRoute</code>、<code>provider.listPublished</code>。
     */
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

    /**
     * excludes已禁用或Unknown专题列表处理入口。
     * 主要协作调用：<code>blogs.publishedTopicBySlug</code>、<code>provider.loadByRoute</code>、<code>provider.supports</code>。
     */
    @Test
    void excludesDisabledOrUnknownTopics() {
        when(blogs.publishedTopicBySlug("hidden")).thenReturn(null);
        assertThat(provider.loadByRoute("/blog/topics/hidden")).isEmpty();
        assertThat(provider.supports("/blog/topics/../../hidden")).isFalse();
    }
}

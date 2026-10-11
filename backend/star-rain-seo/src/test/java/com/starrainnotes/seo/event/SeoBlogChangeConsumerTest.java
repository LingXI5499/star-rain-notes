/*
 * SeoBlogChangeConsumerTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogPostChangedEvent、BlogTopicChangedEvent、ProfilePublicApi、SeoRefreshApi、SeoNotificationService、SeoTutorialRefreshService。
 * 阅读入口：renamedPublishedPostRemovesOldUrlAndRefreshesNewUrl（renamed已发布文章RemovesOld地址及RefreshesNew地址处理入口）；disabledTopicIsRemovedFromSitemap（已禁用专题是否已移除来源Sitemap处理入口）。
 */

package com.starrainnotes.seo.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.api.event.BlogTopicChangedEvent;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.service.SeoTutorialRefreshService;
import org.junit.jupiter.api.Test;

/**
 * SEO博客Change消费者测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoBlogChangeConsumerTest {
    /**
     * 刷新。
     * 协作类型：<code>SeoRefreshApi</code>。
     */
    private final SeoRefreshApi refresh = mock(SeoRefreshApi.class);
    /**
     * notifications。
     * 协作类型：<code>SeoNotificationService</code>。
     */
    private final SeoNotificationService notifications = mock(SeoNotificationService.class);
    /**
     * 消费者。
     */
    private final SeoContentEventConsumer consumer = new SeoContentEventConsumer(refresh,
            mock(SeoTutorialRefreshService.class), notifications, mock(ProfilePublicApi.class));

    /**
     * renamed已发布文章RemovesOld地址及RefreshesNew地址处理入口。
     * 主要协作调用：<code>consumer.onBlogChanged</code>。
     */
    @Test
    void renamedPublishedPostRemovesOldUrlAndRefreshesNewUrl() {
        consumer.onBlogChanged(new BlogPostChangedEvent(9L, "old-post", "new-post"));

        verify(refresh).removeRoute("/blog/posts/old-post");
        verify(refresh).refreshRoute("/blog/posts/new-post");
        verify(notifications).enqueue("/blog/posts/old-post", "DELETE");
        verify(notifications).enqueue("/blog/posts/new-post", "UPSERT");
    }

    /**
     * 已禁用专题是否已移除来源Sitemap处理入口。
     * 主要协作调用：<code>consumer.onBlogTopicChanged</code>。
     */
    @Test
    void disabledTopicIsRemovedFromSitemap() {
        consumer.onBlogTopicChanged(new BlogTopicChangedEvent(3L, "java", "java", false));

        verify(refresh).removeRoute("/blog/topics/java");
        verify(notifications).enqueue("/blog/topics/java", "DELETE");
    }
}

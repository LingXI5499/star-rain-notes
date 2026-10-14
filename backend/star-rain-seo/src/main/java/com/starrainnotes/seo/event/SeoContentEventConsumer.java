/*
 * SeoContentEventConsumer：公开页面与 SEO模块的事件协作文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 * 主要协作类型：BlogPostPublishedEvent、BlogPostWithdrawnEvent、BlogPostChangedEvent、BlogTopicChangedEvent、WorkPublicationChangedEvent、ProfilePublicApi、ProfileChangedEvent。
 * 阅读入口：onSiteConfigChanged（响应站点配置Changed）；onBlogPublished（响应博客已发布）；onBlogWithdrawn（响应博客已撤回）；onBlogChanged（响应博客Changed）。
 */

package com.starrainnotes.seo.event;

import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.api.event.BlogPostWithdrawnEvent;
import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.api.event.BlogTopicChangedEvent;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.event.ProfileChangedEvent;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.service.SeoTutorialRefreshService;
import com.starrainnotes.site.api.event.SiteConfigChangedEvent;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * SEO内容事件消费者，属于公开页面与 SEO模块。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 */
@Component
@RequiredArgsConstructor
public class SeoContentEventConsumer {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationEventPublisher acknowledgements;
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SeoContentEventConsumer.class);
    /**
     * 刷新。
     * 协作类型：<code>SeoRefreshApi</code>。
     */
    private final SeoRefreshApi refresh;
    /**
     * 教程刷新。
     * 协作类型：<code>SeoTutorialRefreshService</code>。
     */
    private final SeoTutorialRefreshService tutorialRefresh;
    /**
     * notifications。
     * 协作类型：<code>SeoNotificationService</code>。
     */
    private final SeoNotificationService notifications;
    /**
     * profiles。
     * 协作类型：<code>ProfilePublicApi</code>。
     */
    private final ProfilePublicApi profiles;

    /**
     * 响应站点配置Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>upsert</code>。
     *
     * @param event 事件；类型为 <code>SiteConfigChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onSiteConfigChanged(SiteConfigChangedEvent event) {
        safe(event, "SITE", null, () -> upsert("/"));
    }

    /**
     * 响应博客已发布。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>event.getPostId</code>、<code>upsert</code>、<code>event.getSlug</code>。
     *
     * @param event 事件；类型为 <code>BlogPostPublishedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onBlogPublished(BlogPostPublishedEvent event) {
        safe(event, "BLOG", event.getPostId(), () -> upsert("/blog/posts/" + event.getSlug()));
    }

    /**
     * 响应博客已撤回。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>event.getPostId</code>、<code>remove</code>、<code>event.getSlug</code>。
     *
     * @param event 事件；类型为 <code>BlogPostWithdrawnEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onBlogWithdrawn(BlogPostWithdrawnEvent event) {
        safe(event, "BLOG", event.getPostId(), () -> remove("/blog/posts/" + event.getSlug()));
    }

    /**
     * 响应博客Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>event.postId</code>、<code>event.previousSlug</code>、<code>event.slug</code>。
     *
     * @param event 事件；类型为 <code>BlogPostChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onBlogChanged(BlogPostChangedEvent event) {
        safe(event, "BLOG", event.postId(), () -> {
            if (!event.previousSlug().equals(event.slug())) {
                remove("/blog/posts/" + event.previousSlug());
            }
            upsert("/blog/posts/" + event.slug());
        });
    }

    /**
     * 响应博客专题Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>event.topicId</code>、<code>event.slug</code>、<code>event.previousSlug</code>。
     *
     * @param event 事件；类型为 <code>BlogTopicChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onBlogTopicChanged(BlogTopicChangedEvent event) {
        safe(event, "BLOG_TOPIC", event.topicId(), () -> {
            String path = "/blog/topics/" + event.slug();
            if (event.previousSlug() != null && !event.previousSlug().equals(event.slug())) {
                remove("/blog/topics/" + event.previousSlug());
            }
            if (event.visible()) upsert(path);
            else remove(path);
        });
    }

    /**
     * 响应作品Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 关键处理步骤调用 <code>safe</code>、<code>event.getWorkId</code>、<code>event.getSlug</code>、<code>event.isPublished</code>。
     *
     * @param event 事件；类型为 <code>WorkPublicationChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onWorkChanged(WorkPublicationChangedEvent event) {
        safe(event, "PORTFOLIO", event.getWorkId(), () -> {
            String path = "/portfolio/" + event.getSlug();
            if (event.isPublished()) upsert(path);
            else remove(path);
        });
    }

    /**
     * 响应资料Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 主要协作调用：<code>profiles.publicProfileId</code>。
     *
     * @param event 事件；类型为 <code>ProfileChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onProfileChanged(ProfileChangedEvent event) {
        safe(event, "PROFILE", event.getProfileId(), () -> {
            if (profiles.publicProfileId() == null) remove("/about");
            else upsert("/about");
        });
    }

    /**
     * 响应教程Changed。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 主要协作调用：<code>tutorialRefresh.publicationChanged</code>。
     *
     * @param event 事件；类型为 <code>TutorialPublicationChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onTutorialChanged(TutorialPublicationChangedEvent event) {
        safe(event, "TUTORIAL", event.getTutorialId(),
            () -> tutorialRefresh.publicationChanged(event.getSlug(), event.getAction()));
    }

    /**
     * 新增或更新SEO内容事件消费者。
     * 主要协作调用：<code>refresh.refreshRoute</code>、<code>notifications.enqueue</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     */
    private void upsert(String path) {
        refresh.refreshRoute(path);
        notifications.enqueue(path, "UPSERT");
    }

    /**
     * 移除SEO内容事件消费者。
     * 主要协作调用：<code>refresh.removeRoute</code>、<code>notifications.enqueue</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     */
    private void remove(String path) {
        refresh.removeRoute(path);
        notifications.enqueue(path, "DELETE");
    }

    /**
     * 安全处理入口。
     * 关键处理步骤调用 <code>action.run</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>Runnable</code>
     */
    private void safe(Object event, String type, Long id, Runnable action) {
        boolean success = false;
        try { action.run(); success = true; }
        catch (RuntimeException failure) { log.error("SEO projection sync failed for {} {}", type, id, failure); }
        finally {
            if (acknowledgements != null) {
                try { acknowledgements.publishEvent(new com.starrainnotes.common.event.ProjectionHandledEvent(event, "SEO", success)); }
                catch (RuntimeException failure) { log.warn("SEO acknowledgement failed; durable repair remains pending", failure); }
            }
        }
    }
}

/*
 * SearchEventConsumer：搜索模块的事件协作文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 * 主要协作类型：BlogPostPublishedEvent、BlogPostWithdrawnEvent、BlogPostChangedEvent、BlogSearchSourceApi、EnglishSearchContentChangedEvent、WorkPublicationChangedEvent、PortfolioReferenceApi。
 * 阅读入口：onEnglishChanged（响应英语Changed）；onBlogPublished（响应博客已发布）；onBlogWithdrawn（响应博客已撤回）；onBlogChanged（响应博客Changed）。
 */

package com.starrainnotes.search.event;

import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.api.event.BlogPostWithdrawnEvent;
import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.profile.api.event.ProfileChangedEvent;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 搜索事件消费者，属于搜索模块。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 */
@Component
@RequiredArgsConstructor
public class SearchEventConsumer {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SearchEventConsumer.class);
    /**
     * rebuild。
     * 协作类型：<code>SearchRebuildService</code>。
     */
    private final SearchRebuildService rebuild;
    /**
     * 索引。
     * 协作类型：<code>SearchIndexService</code>。
     */
    private final SearchIndexService index;
    /**
     * blogs。
     * 协作类型：<code>BlogSearchSourceApi</code>。
     */
    private final BlogSearchSourceApi blogs;
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioReferenceApi</code>。
     */
    private final PortfolioReferenceApi works;

    /**
     * 响应英语Changed。
     * 事务声明：<code>@TransactionalEventListener(fallbackExecution = true)</code>，该方法的数据库操作按此事务配置执行。
     * 调用约束：<code>@TransactionalEventListener(fallbackExecution = true)</code>。
     * 主要协作调用：<code>rebuild.rebuildType</code>、<code>rebuild.syncEnglish</code>。
     *
     * @param event 事件；类型为 <code>EnglishSearchContentChangedEvent</code>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onEnglishChanged(EnglishSearchContentChangedEvent event) {
        safe(event.getContentType(), event.getContentId(), () -> {
            if (event.getContentId() == null) rebuild.rebuildType(event.getContentType());
            else rebuild.syncEnglish(event.getContentType(), event.getContentId());
        });
    }

    /**
     * 响应博客已发布。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>blogs.getPublishedDocument</code>、<code>rebuild.indexBlog</code>。
     *
     * @param event 事件；类型为 <code>BlogPostPublishedEvent</code>
     */
    @EventListener
    public void onBlogPublished(BlogPostPublishedEvent event) {
        safe("BLOG", event.getPostId(), () -> {
            var post = blogs.getPublishedDocument(event.getPostId());
            if (post != null) rebuild.indexBlog(post);
        });
    }

    /**
     * 响应博客已撤回。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>index.removeByContent</code>。
     *
     * @param event 事件；类型为 <code>BlogPostWithdrawnEvent</code>
     */
    @EventListener
    public void onBlogWithdrawn(BlogPostWithdrawnEvent event) {
        safe("BLOG", event.getPostId(), () -> index.removeByContent("BLOG", event.getPostId()));
    }

    /**
     * 响应博客Changed。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>blogs.getPublishedDocument</code>、<code>rebuild.indexBlog</code>。
     *
     * @param event 事件；类型为 <code>BlogPostChangedEvent</code>
     */
    @EventListener
    public void onBlogChanged(BlogPostChangedEvent event) {
        safe("BLOG", event.postId(), () -> {
            var post = blogs.getPublishedDocument(event.postId());
            if (post != null) rebuild.indexBlog(post);
        });
    }

    /**
     * 响应教程Changed。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>rebuild.rebuildType</code>。
     *
     * @param event 事件；类型为 <code>TutorialPublicationChangedEvent</code>
     */
    @EventListener
    public void onTutorialChanged(TutorialPublicationChangedEvent event) {
        safe("TUTORIAL", event.getTutorialId(), () -> rebuild.rebuildType("TUTORIAL"));
    }

    /**
     * 响应作品Changed。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>works.publishedWork</code>、<code>index.removeByContent</code>。
     *
     * @param event 事件；类型为 <code>WorkPublicationChangedEvent</code>
     */
    @EventListener
    public void onWorkChanged(WorkPublicationChangedEvent event) {
        safe("PORTFOLIO", event.getWorkId(), () -> {
            if (event.isPublished()) works.publishedWork(event.getWorkId()).ifPresent(rebuild::indexWork);
            else index.removeByContent("PORTFOLIO", event.getWorkId());
        });
    }

    /**
     * 响应资料Changed。
     * 调用约束：<code>@EventListener</code>。
     * 主要协作调用：<code>rebuild.rebuildType</code>。
     *
     * @param event 事件；类型为 <code>ProfileChangedEvent</code>
     */
    @EventListener
    public void onProfileChanged(ProfileChangedEvent event) {
        safe("PROFILE", event.getProfileId(), () -> rebuild.rebuildType("PROFILE"));
    }

    /**
     * 安全处理入口。
     * 关键处理步骤调用 <code>action.run</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>Runnable</code>
     */
    private void safe(String type, Long id, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Index is derived state: a failed consumer must not turn a committed publication into a failed HTTP response.
            log.error("Search index sync failed for {} {}; run rebuild", type, id, exception);
        }
    }
}

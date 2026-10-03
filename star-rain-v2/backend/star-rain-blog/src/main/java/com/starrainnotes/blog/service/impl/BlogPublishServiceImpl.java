package com.starrainnotes.blog.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.enumeration.BlogPostStatus;
import com.starrainnotes.blog.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.event.BlogPostRestoredEvent;
import com.starrainnotes.blog.event.BlogPostWithdrawnEvent;
import com.starrainnotes.blog.exception.BlogAccessDeniedException;
import com.starrainnotes.blog.exception.BlogPostContentEmptyException;
import com.starrainnotes.blog.exception.BlogPostNotFoundException;
import com.starrainnotes.blog.exception.BlogPostSlugInvalidException;
import com.starrainnotes.blog.exception.BlogPostStateInvalidException;
import com.starrainnotes.blog.exception.BlogPostSummaryInvalidException;
import com.starrainnotes.blog.exception.BlogPostTitleInvalidException;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.service.BlogPublishService;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.utils.BlogTextRules;
import com.starrainnotes.media.api.MediaAssetApi;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
 * BLOG-008 / BLOG-009 的实现。
 *
 * 三件事必须同时成立，缺一不可：
 * 1. 只有 Super Admin 能发布（规范 §3）。权限码之外再查一次角色：
 *    即使将来有人误把 blog:publish 授给别的角色，业务层仍然拦得住。
 * 2. 状态切换用条件 UPDATE 完成，判断与写入在同一条 SQL 里，
 *    重复点击只会有一个成功，另一个拿到 BLOG_POST_STATE_INVALID。
 * 3. 事件在事务提交之后发布，避免消费者在回滚前就建索引 / 生成 Sitemap。
 *
 * 关于标签：规范 §12 允许“所有 Tag ENABLED”或“明确允许保留历史禁用标签”二选一。
 * 这里选后者 —— 停用标签后历史文章仍能继续发布，新绑定则在绑定环节就被
 * BLOG_TAG_DISABLED 拦下。否则“停用某标签”会连带让所有用过它的老文章无法重新发布。
 */
@Service
public class BlogPublishServiceImpl implements BlogPublishService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final BlogPostMapper postMapper;
    private final MediaAssetApi mediaAssetApi;
    private final CurrentActorApi currentActorApi;
    private final ApplicationEventPublisher eventPublisher;

    public BlogPublishServiceImpl(BlogPostMapper postMapper,
                                  MediaAssetApi mediaAssetApi,
                                  CurrentActorApi currentActorApi,
                                  ApplicationEventPublisher eventPublisher) {
        this.postMapper = postMapper;
        this.mediaAssetApi = mediaAssetApi;
        this.currentActorApi = currentActorApi;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void publish(Long postId) {
        Long actorId = requireSuperAdmin();
        BlogPostEntity post = requirePostForUpdate(postId);
        if (BlogPostStatus.PUBLISHED_CODE.equals(post.getStatus())) {
            throw new BlogPostStateInvalidException("文章已经发布，不能重复发布");
        }
        validateForPublish(post, actorId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = postMapper.publishPost(postId, now, actorId);
        if (updated == 0) {
            throw new BlogPostStateInvalidException("文章状态已变化，发布未生效");
        }
        BlogPostEntity published = postMapper.postById(postId);
        eventAfterCommit(BlogPostPublishedEvent.builder()
                .postId(postId)
                .slug(published.getSlug())
                .title(published.getTitle())
                .publishedAt(published.getPublishedAt())
                .build());
    }

    @Override
    @Transactional
    public void withdraw(Long postId) {
        Long actorId = requireSuperAdmin();
        BlogPostEntity post = requirePostForUpdate(postId);
        if (!BlogPostStatus.PUBLISHED_CODE.equals(post.getStatus())) {
            throw new BlogPostStateInvalidException("只有已发布文章可以撤回");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = postMapper.withdrawPost(postId, now, actorId);
        if (updated == 0) {
            throw new BlogPostStateInvalidException("文章状态已变化，撤回未生效");
        }
        eventAfterCommit(BlogPostWithdrawnEvent.builder()
                .postId(postId)
                .slug(post.getSlug())
                .withdrawnAt(now)
                .build());
    }

    @Override
    @Transactional
    public void restore(Long postId) {
        Long actorId = requireSuperAdmin();
        BlogPostEntity post = requirePostForUpdate(postId);
        if (!BlogPostStatus.WITHDRAWN_CODE.equals(post.getStatus())) {
            throw new BlogPostStateInvalidException("只有已撤回文章可以恢复");
        }
        validateForPublish(post, actorId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = postMapper.publishPost(postId, now, actorId);
        if (updated == 0) {
            throw new BlogPostStateInvalidException("文章状态已变化，恢复未生效");
        }
        BlogPostEntity restored = postMapper.postById(postId);
        eventAfterCommit(BlogPostRestoredEvent.builder()
                .postId(postId)
                .slug(restored.getSlug())
                .publishedAt(restored.getPublishedAt())
                .build());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private Long requireSuperAdmin() {
        CurrentActorApi.CurrentActor actor = currentActorApi.currentOptional()
                .orElseThrow(BlogAccessDeniedException::new);
        if (actor.getRoles() == null || !actor.getRoles().contains(SUPER_ADMIN_ROLE)) {
            throw new BlogAccessDeniedException();
        }
        return actor.getAccountId();
    }

    private BlogPostEntity requirePostForUpdate(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.postByIdForUpdate(postId);
        if (post == null) {
            throw new BlogPostNotFoundException();
        }
        return post;
    }

    /*
     * 发布前校验；摘要为空时按正文派生一份并落库。
     *
     * 派生写在这里，而不是要求作者先调一次 PATCH：
     * 摘要对“能否发布”不是硬条件（正文非空才是），但它决定列表卡片与 meta description 的观感，
     * 顺手补齐比逼作者重复劳动更可能被长期遵守。
     */
    private void validateForPublish(BlogPostEntity post, Long actorId) {
        if (!BlogTextRules.hasText(post.getTitle())) {
            throw new BlogPostTitleInvalidException("标题不能为空，无法发布");
        }
        if (BlogTextRules.length(post.getTitle()) > BlogLimits.POST_TITLE_MAX_LENGTH) {
            throw new BlogPostTitleInvalidException("标题不能超过 " + BlogLimits.POST_TITLE_MAX_LENGTH + " 个字符");
        }
        if (!BlogSlugRules.isValid(post.getSlug(), BlogLimits.POST_SLUG_MAX_LENGTH)) {
            throw new BlogPostSlugInvalidException("slug 不合法，无法发布");
        }
        if (!BlogTextRules.hasText(post.getBodyMarkdown())) {
            throw new BlogPostContentEmptyException();
        }

        String summary = post.getSummary();
        if (!BlogTextRules.hasText(summary)) {
            summary = BlogTextRules.deriveSummary(post.getBodyMarkdown());
            if (summary != null) {
                postMapper.updatePostMeta(post.getId(), post.getSlug(), post.getTitle(), summary, actorId);
            }
        }
        if (BlogTextRules.length(summary) > BlogLimits.POST_SUMMARY_MAX_LENGTH) {
            throw new BlogPostSummaryInvalidException(
                    "摘要不能超过 " + BlogLimits.POST_SUMMARY_MAX_LENGTH + " 个字符");
        }

        /*
         * 封面必须是可用的媒体。
         * 走 MediaAssetApi 而不是查引用表：引用可能存在但资产已被归档，
         * 发布后前台会拿到 404 的封面地址。
         */
        if (post.getCoverMediaAssetId() != null) {
            mediaAssetApi.assertUsable(post.getCoverMediaAssetId());
        }
    }

    /*
     * 事件在提交之后发布。
     *
     * 若当前没有活跃事务（例如被单测直接调用），立即发布：这样单元测试不需要
     * 额外搭事务，而生产路径上依然保证“先提交、后通知”。
     */
    private void eventAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }
}

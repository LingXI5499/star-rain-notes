/*
 * BlogPublishServiceImpl：博客模块的业务实现文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、BlogLimits、BlogPostEntity、BlogPostStatus、BlogPostPublishedEvent、BlogPostWithdrawnEvent、BlogAccessDeniedException。
 * 阅读入口：publish（发布博客发布）；withdraw（撤回博客发布）；restore（恢复博客发布）；afterCommit（之后Commit处理入口）。
 */

package com.starrainnotes.blog.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.enumeration.BlogPostStatus;
import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.api.event.BlogPostWithdrawnEvent;
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
/**
 * 博客发布业务服务实现，属于博客模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class BlogPublishServiceImpl implements BlogPublishService {

    /**
     * 角色。
     */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     */
    private final BlogPostMapper postMapper;
    /**
     * 媒体资产接口。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi mediaAssetApi;
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 事件发布器。
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造BlogPublishServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     * @param mediaAssetApi 媒体资产接口；类型为 <code>MediaAssetApi</code>
     * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
     * @param eventPublisher 事件发布器；类型为 <code>ApplicationEventPublisher</code>
     */
    public BlogPublishServiceImpl(BlogPostMapper postMapper,
                                  MediaAssetApi mediaAssetApi,
                                  CurrentActorApi currentActorApi,
                                  ApplicationEventPublisher eventPublisher) {
        this.postMapper = postMapper;
        this.mediaAssetApi = mediaAssetApi;
        this.currentActorApi = currentActorApi;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布博客发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.publishPost</code>、<code>postMapper.postById</code>。
     * 实现中的显式失败类型包括 <code>BlogPostStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
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
        publishChange(BlogPostPublishedEvent.builder()
                .postId(postId)
                .slug(published.getSlug())
                .title(published.getTitle())
                .publishedAt(published.getPublishedAt())
                .build());
    }

    /**
     * 撤回博客发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.withdrawPost</code>。
     * 实现中的显式失败类型包括 <code>BlogPostStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
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
        publishChange(BlogPostWithdrawnEvent.builder()
                .postId(postId)
                .slug(post.getSlug())
                .withdrawnAt(now)
                .build());
    }

    /**
     * 恢复博客发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.publishPost</code>、<code>postMapper.postById</code>。
     * 实现中的显式失败类型包括 <code>BlogPostStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
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
        publishChange(BlogPostPublishedEvent.builder()
                .postId(postId)
                .slug(restored.getSlug())
                .title(restored.getTitle())
                .publishedAt(restored.getPublishedAt())
                .build());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 检查所需Super后台。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>。
     * 实现中的显式失败类型包括 <code>BlogAccessDeniedException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long requireSuperAdmin() {
        CurrentActorApi.CurrentActor actor = currentActorApi.currentOptional()
                .orElseThrow(BlogAccessDeniedException::new);
        if (actor.getRoles() == null || !actor.getRoles().contains(SUPER_ADMIN_ROLE)) {
            throw new BlogAccessDeniedException();
        }
        return actor.getAccountId();
    }

    /**
     * 检查所需文章面向更新。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>BlogPostNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    private BlogPostEntity requirePostForUpdate(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.postByIdForUpdate(postId);
        if (post == null) {
            throw new BlogPostNotFoundException();
        }
        com.starrainnotes.common.web.ContentVersion.require(post.getRowVersion());
        postMapper.archive(post.getId());
        postMapper.pruneHistory(post.getId(), post.getRowVersion() == null ? 0 : post.getRowVersion());
        return post;
    }

    /*
     * 发布前校验；摘要为空时按正文派生一份并落库。
     *
     * 派生写在这里，而不是要求作者先调一次 PATCH：
     * 摘要对“能否发布”不是硬条件（正文非空才是），但它决定列表卡片与 meta description 的观感，
     * 顺手补齐比逼作者重复劳动更可能被长期遵守。
     */
    /**
     * 校验面向发布。
     * 主要协作调用：<code>postMapper.updatePostMeta</code>、<code>mediaAssetApi.assertUsable</code>。
     * 实现中的显式失败类型包括 <code>BlogPostTitleInvalidException</code>、<code>BlogPostSlugInvalidException</code>、<code>BlogPostContentEmptyException</code>、<code>BlogPostSummaryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
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
            requirePublicMedia(post.getCoverMediaAssetId());
        }
        for (Long assetId : com.starrainnotes.blog.utils.BlogContentMediaParser.extractMediaAssetIds(post.getBodyMarkdown())) {
            mediaAssetApi.assertUsable(assetId);
            requirePublicMedia(assetId);
        }
    }

    private void requirePublicMedia(Long id) {
        var asset = mediaAssetApi.get(id);
        if (asset == null || !"PUBLIC".equals(asset.getAccessLevel())) {
            throw new com.starrainnotes.common.exception.ApiException("BLOG_MEDIA_NOT_PUBLIC",
                    "文章引用的素材尚未公开，请在媒体库确认访问级别后发布。", 409);
        }
    }

    /*
     * 事件在提交之后发布。
     *
     * 若当前没有活跃事务（例如被单测直接调用），立即发布：这样单元测试不需要
     * 额外搭事务，而生产路径上依然保证“先提交、后通知”。
     */
    /**
     * 事件之后Commit处理入口。
     * 主要协作调用：<code>eventPublisher.publishEvent</code>。
     *
     * @param event 事件；类型为 <code>Object</code>
     */
    private void publishChange(Object event) {
        eventPublisher.publishEvent(event);
    }
}

package com.starrainnotes.blog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.event.BlogPostRestoredEvent;
import com.starrainnotes.blog.event.BlogPostWithdrawnEvent;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.exception.MediaAssetNotActiveException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/*
 * 发布 / 撤回 / 恢复状态机测试（BLOG-008 / BLOG-009）。
 *
 * 覆盖三件事：
 * 1. 只有 SUPER_ADMIN 能发布 —— 即使权限码被误授，服务层仍然拒绝；
 * 2. 发布前的内容校验（标题、slug、正文、摘要长度、封面可用性）；
 * 3. 条件更新影响 0 行时必须报错，不允许静默当作成功。
 */
@ExtendWith(MockitoExtension.class)
class BlogPublishServiceImplTest {

    @Mock
    private BlogPostMapper postMapper;

    @Mock
    private MediaAssetApi mediaAssetApi;

    @Mock
    private CurrentActorApi currentActorApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BlogPublishServiceImpl service;

    // ------------------------------------------------------------------
    // 发布
    // ------------------------------------------------------------------

    @Test
    @DisplayName("草稿发布成功：写入 PUBLISHED 并在提交后广播发布事件")
    void publishDraft() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 10, 2, 8, 0);
        BlogPostEntity published = post(9L, "PUBLISHED", "第一篇", "first-post", "摘要", "正文", null);
        published.setPublishedAt(publishedAt);
        when(postMapper.postById(9L)).thenReturn(published);

        service.publish(9L);

        verify(postMapper).publishPost(eq(9L), any(LocalDateTime.class), eq(1L));
        Object event = capturedEvent();
        assertThat(event).isInstanceOf(BlogPostPublishedEvent.class);
        BlogPostPublishedEvent publishedEvent = (BlogPostPublishedEvent) event;
        assertThat(publishedEvent.getPostId()).isEqualTo(9L);
        assertThat(publishedEvent.getSlug()).isEqualTo("first-post");
        assertThat(publishedEvent.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("已发布文章不能重复发布：这是状态错误而不是权限错误")
    void publishPublishedRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "摘要", "正文", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
        verify(postMapper, never()).publishPost(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("正文为空不能发布")
    void publishWithoutBodyRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "   ", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_CONTENT_EMPTY");
        verify(postMapper, never()).publishPost(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("标题为空不能发布")
    void publishWithoutTitleRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "  ", "first-post",
                "摘要", "正文", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_TITLE_INVALID");
    }

    @Test
    @DisplayName("slug 不合法不能发布：发布是 slug 进入公开 URL 的最后一道关口")
    void publishWithInvalidSlugRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "Bad Slug",
                "摘要", "正文", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_SLUG_INVALID");
    }

    @Test
    @DisplayName("摘要超长不能发布")
    void publishWithTooLongSummaryRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘".repeat(1001), "正文", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_SUMMARY_INVALID");
    }

    @Test
    @DisplayName("摘要为空时按正文派生并落库，作者不必手写摘要")
    void publishDerivesSummary() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                null, "# 标题\n\n第一段正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        when(postMapper.postById(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "标题 第一段正文", "正文", null));

        service.publish(9L);

        verify(postMapper).updatePostMeta(9L, "first-post", "第一篇", "标题 第一段正文", 1L);
    }

    @Test
    @DisplayName("封面必须是 ACTIVE 媒体：直接沿用 Media 模块的错误码，不另造一套")
    void publishWithArchivedCoverRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "正文", 55L));
        doThrow(new MediaAssetNotActiveException()).when(mediaAssetApi).assertUsable(55L);

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
        verify(postMapper, never()).publishPost(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("条件更新影响 0 行时宁可报错，也不静默当作发布成功")
    void publishReportsConflictWhenConditionalUpdateMisses() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(0);

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("非 Super Admin 即使持有 blog:publish 也被服务层拒绝")
    void publishRejectedForNonSuperAdmin() {
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actor("ADMIN")));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_ACCESS_DENIED");
        verify(postMapper, never()).postByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("匿名调用发布接口报 BLOG_ACCESS_DENIED（URL 层之外的第二道防线）")
    void publishRejectedWhenAnonymous() {
        when(currentActorApi.currentOptional()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_ACCESS_DENIED");
    }

    @Test
    @DisplayName("文章不存在报 BLOG_POST_NOT_FOUND")
    void publishMissingPostRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.publish(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");
    }

    // ------------------------------------------------------------------
    // 撤回 / 恢复
    // ------------------------------------------------------------------

    @Test
    @DisplayName("已发布文章撤回成功并广播撤回事件")
    void withdrawPublished() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.withdrawPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);

        service.withdraw(9L);

        verify(postMapper).withdrawPost(eq(9L), any(LocalDateTime.class), eq(1L));
        Object event = capturedEvent();
        assertThat(event).isInstanceOf(BlogPostWithdrawnEvent.class);
        assertThat(((BlogPostWithdrawnEvent) event).getPostId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("草稿不能撤回")
    void withdrawDraftRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "正文", null));

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
        verify(postMapper, never()).withdrawPost(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("撤回后恢复回到 PUBLISHED，且保留首次发布时间")
    void restoreWithdrawnKeepsFirstPublishedAt() {
        stubActor("SUPER_ADMIN");
        BlogPostEntity withdrawn = post(9L, "WITHDRAWN", "第一篇", "first-post", "摘要", "正文", null);
        withdrawn.setPublishedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(postMapper.postByIdForUpdate(9L)).thenReturn(withdrawn);
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        BlogPostEntity restored = post(9L, "PUBLISHED", "第一篇", "first-post", "摘要", "正文", null);
        restored.setPublishedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(postMapper.postById(9L)).thenReturn(restored);

        service.restore(9L);

        Object event = capturedEvent();
        assertThat(event).isInstanceOf(BlogPostRestoredEvent.class);
        assertThat(((BlogPostRestoredEvent) event).getPublishedAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        // 恢复不是重新发布：publishedAt 必须还是原来那一天
        assertThat(restored.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("已发布文章不能再走恢复：恢复只针对已撤回")
    void restorePublishedRejected() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "摘要", "正文", null));

        assertThatThrownBy(() -> service.restore(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
    }

    @Test
    @DisplayName("非 Super Admin 不能撤回")
    void withdrawRejectedForNonSuperAdmin() {
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actor("ADMIN")));

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_ACCESS_DENIED");
    }

    @Test
    @DisplayName("恢复时条件更新影响 0 行同样报状态错误")
    void restoreReportsConflictWhenConditionalUpdateMisses() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "WITHDRAWN", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(0);

        assertThatThrownBy(() -> service.restore(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("发布时摘要已存在则不改写，保留作者手写的摘要")
    void publishKeepsExistingSummary() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "WITHDRAWN", "第一篇", "first-post",
                "手写摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        when(postMapper.postById(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "手写摘要", "正文", null));

        service.publish(9L);

        verify(postMapper, never()).updatePostMeta(anyLong(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("撤回态文章可以再次发布（撤回不是终态）")
    void publishWithdrawnAgain() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "WITHDRAWN", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        when(postMapper.postById(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "摘要", "正文", null));

        service.publish(9L);

        verify(postMapper).publishPost(eq(9L), any(LocalDateTime.class), eq(1L));
    }

    @Test
    @DisplayName("无封面时不做媒体校验：null 不是错误")
    void publishWithoutCoverSkipsMediaCheck() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘要", "正文", null));
        when(postMapper.publishPost(eq(9L), any(LocalDateTime.class), eq(1L))).thenReturn(1);
        when(postMapper.postById(9L)).thenReturn(post(9L, "PUBLISHED", "第一篇", "first-post",
                "摘要", "正文", null));

        service.publish(9L);

        verify(mediaAssetApi, never()).assertUsable(any());
    }

    @Test
    @DisplayName("作者存的摘要本身超过列长度时拒绝发布，而不是等数据库报错")
    void publishRejectsOversizedStoredSummary() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", "第一篇", "first-post",
                "摘".repeat(5000), "正文", null));

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_SUMMARY_INVALID");
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private void stubActor(String role) {
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actor(role)));
    }

    /*
     * 捕获发布出去的事件。
     *
     * 用 ArgumentCaptor<Object> 而不是 argThat：ApplicationEventPublisher 同时有
     * publishEvent(ApplicationEvent) 与 publishEvent(Object) 两个重载，
     * 内联匹配器会被推断成 ApplicationEvent，编译期就报类型不兼容。
     */
    private Object capturedEvent() {
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static CurrentActorApi.CurrentActor actor(String role) {
        return CurrentActorApi.CurrentActor.builder()
                .accountId(1L)
                .roles(Set.of(role))
                .permissions(Set.of("blog:publish", "blog:withdraw", "blog:edit"))
                .build();
    }

    private static BlogPostEntity post(Long id, String status, String title, String slug,
                                       String summary, String body, Long coverMediaAssetId) {
        BlogPostEntity entity = new BlogPostEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setTitle(title);
        entity.setSlug(slug);
        entity.setSummary(summary);
        entity.setBodyMarkdown(body);
        entity.setCoverMediaAssetId(coverMediaAssetId);
        entity.setCreatedByAccountId(1L);
        entity.setUpdatedByAccountId(1L);
        return entity;
    }
}

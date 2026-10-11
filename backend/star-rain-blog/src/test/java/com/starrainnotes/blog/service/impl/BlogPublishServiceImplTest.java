/*
 * BlogPublishServiceImplTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、BlogPostEntity、BlogPostPublishedEvent、BlogPostWithdrawnEvent、BlogPostMapper、ApiException、MediaAssetApi。
 * 阅读入口：publishDraft（\u8349\u7a3f\u53d1\u5e03\u6210\u529f\uff1a\u5199\u5165 PUBLISHED \u5e76\u5728\u63d0\u4ea4\u540e\u5e7f\u64ad\u53d1\u5e03\u4e8b\u4ef6）；publishPublishedRejected（\u5df2\u53d1\u5e03\u6587\u7ae0\u4e0d\u80fd\u91cd\u590d\u53d1\u5e03\uff1a\u8fd9\u662f\u72b6\u6001\u9519\u8bef\u800c\u4e0d\u662f\u6743\u9650\u9519\u8bef）；publishWithoutBodyRejected（\u6b63\u6587\u4e3a\u7a7a\u4e0d\u80fd\u53d1\u5e03）；publishWithoutTitleRejected（\u6807\u9898\u4e3a\u7a7a\u4e0d\u80fd\u53d1\u5e03）。
 */

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
import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.api.event.BlogPostWithdrawnEvent;
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
/**
 * 博客发布业务服务实现测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class BlogPublishServiceImplTest {

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogPostMapper postMapper;

    /**
     * 媒体资产接口。
     * 协作类型：<code>MediaAssetApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaAssetApi mediaAssetApi;

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private CurrentActorApi currentActorApi;

    /**
     * 事件发布器。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private ApplicationEventPublisher eventPublisher;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks
    private BlogPublishServiceImpl service;

    // ------------------------------------------------------------------
    // 发布
    // ------------------------------------------------------------------

    /**
     * \u8349\u7a3f\u53d1\u5e03\u6210\u529f\uff1a\u5199\u5165 PUBLISHED \u5e76\u5728\u63d0\u4ea4\u540e\u5e7f\u64ad\u53d1\u5e03\u4e8b\u4ef6。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.publish</code>。
     */
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

    /**
     * \u5df2\u53d1\u5e03\u6587\u7ae0\u4e0d\u80fd\u91cd\u590d\u53d1\u5e03\uff1a\u8fd9\u662f\u72b6\u6001\u9519\u8bef\u800c\u4e0d\u662f\u6743\u9650\u9519\u8bef。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u6b63\u6587\u4e3a\u7a7a\u4e0d\u80fd\u53d1\u5e03。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u6807\u9898\u4e3a\u7a7a\u4e0d\u80fd\u53d1\u5e03。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * slug \u4e0d\u5408\u6cd5\u4e0d\u80fd\u53d1\u5e03\uff1a\u53d1\u5e03\u662f slug \u8fdb\u5165\u516c\u5f00 URL \u7684\u6700\u540e\u4e00\u9053\u5173\u53e3。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u6458\u8981\u8d85\u957f\u4e0d\u80fd\u53d1\u5e03。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u6458\u8981\u4e3a\u7a7a\u65f6\u6309\u6b63\u6587\u6d3e\u751f\u5e76\u843d\u5e93\uff0c\u4f5c\u8005\u4e0d\u5fc5\u624b\u5199\u6458\u8981。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.publish</code>。
     */
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

    /**
     * \u5c01\u9762\u5fc5\u987b\u662f ACTIVE \u5a92\u4f53\uff1a\u76f4\u63a5\u6cbf\u7528 Media \u6a21\u5757\u7684\u9519\u8bef\u7801\uff0c\u4e0d\u53e6\u9020\u4e00\u5957。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u6761\u4ef6\u66f4\u65b0\u5f71\u54cd 0 \u884c\u65f6\u5b81\u53ef\u62a5\u9519\uff0c\u4e5f\u4e0d\u9759\u9ed8\u5f53\u4f5c\u53d1\u5e03\u6210\u529f。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>service.publish</code>。
     */
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

    /**
     * \u975e Super Admin \u5373\u4f7f\u6301\u6709 blog:publish \u4e5f\u88ab\u670d\u52a1\u5c42\u62d2\u7edd。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>、<code>service.publish</code>。
     */
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

    /**
     * \u533f\u540d\u8c03\u7528\u53d1\u5e03\u63a5\u53e3\u62a5 BLOG_ACCESS_DENIED\uff08URL \u5c42\u4e4b\u5916\u7684\u7b2c\u4e8c\u9053\u9632\u7ebf\uff09。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>、<code>service.publish</code>。
     */
    @Test
    @DisplayName("匿名调用发布接口报 BLOG_ACCESS_DENIED（URL 层之外的第二道防线）")
    void publishRejectedWhenAnonymous() {
        when(currentActorApi.currentOptional()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_ACCESS_DENIED");
    }

    /**
     * \u6587\u7ae0\u4e0d\u5b58\u5728\u62a5 BLOG_POST_NOT_FOUND。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * \u5df2\u53d1\u5e03\u6587\u7ae0\u64a4\u56de\u6210\u529f\u5e76\u5e7f\u64ad\u64a4\u56de\u4e8b\u4ef6。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.withdrawPost</code>、<code>service.withdraw</code>。
     */
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

    /**
     * \u8349\u7a3f\u4e0d\u80fd\u64a4\u56de。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.withdraw</code>。
     */
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

    /**
     * \u64a4\u56de\u540e\u6062\u590d\u56de\u5230 PUBLISHED\uff0c\u4e14\u4fdd\u7559\u9996\u6b21\u53d1\u5e03\u65f6\u95f4。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.restore</code>。
     */
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
        assertThat(event).isInstanceOf(BlogPostPublishedEvent.class);
        assertThat(((BlogPostPublishedEvent) event).getPublishedAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        // 恢复不是重新发布：publishedAt 必须还是原来那一天
        assertThat(restored.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    /**
     * \u5df2\u53d1\u5e03\u6587\u7ae0\u4e0d\u80fd\u518d\u8d70\u6062\u590d\uff1a\u6062\u590d\u53ea\u9488\u5bf9\u5df2\u64a4\u56de。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.restore</code>。
     */
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

    /**
     * \u5df2\u64a4\u56de\u6587\u7ae0\u5185\u5bb9\u88ab\u6e05\u7a7a\u540e\u4e0d\u80fd\u76f4\u63a5\u91cd\u65b0\u516c\u5f00。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.restore</code>。
     */
    @Test
    @DisplayName("已撤回文章内容被清空后不能直接重新公开")
    void restoreRejectsEmptyBody() {
        stubActor("SUPER_ADMIN");
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "WITHDRAWN", "第一篇", "first-post",
                "摘要", "", null));

        assertThatThrownBy(() -> service.restore(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_CONTENT_EMPTY");
        verify(postMapper, never()).publishPost(anyLong(), any(), anyLong());
    }

    /**
     * \u975e Super Admin \u4e0d\u80fd\u64a4\u56de。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>、<code>service.withdraw</code>。
     */
    @Test
    @DisplayName("非 Super Admin 不能撤回")
    void withdrawRejectedForNonSuperAdmin() {
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actor("ADMIN")));

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_ACCESS_DENIED");
    }

    /**
     * \u6062\u590d\u65f6\u6761\u4ef6\u66f4\u65b0\u5f71\u54cd 0 \u884c\u540c\u6837\u62a5\u72b6\u6001\u9519\u8bef。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>service.restore</code>。
     */
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

    /**
     * \u53d1\u5e03\u65f6\u6458\u8981\u5df2\u5b58\u5728\u5219\u4e0d\u6539\u5199\uff0c\u4fdd\u7559\u4f5c\u8005\u624b\u5199\u7684\u6458\u8981。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.publish</code>。
     */
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

    /**
     * \u64a4\u56de\u6001\u6587\u7ae0\u53ef\u4ee5\u518d\u6b21\u53d1\u5e03\uff08\u64a4\u56de\u4e0d\u662f\u7ec8\u6001\uff09。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.publish</code>。
     */
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

    /**
     * \u65e0\u5c01\u9762\u65f6\u4e0d\u505a\u5a92\u4f53\u6821\u9a8c\uff1anull \u4e0d\u662f\u9519\u8bef。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.publishPost</code>、<code>postMapper.postById</code>、<code>service.publish</code>。
     */
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

    /**
     * \u4f5c\u8005\u5b58\u7684\u6458\u8981\u672c\u8eab\u8d85\u8fc7\u5217\u957f\u5ea6\u65f6\u62d2\u7edd\u53d1\u5e03\uff0c\u800c\u4e0d\u662f\u7b49\u6570\u636e\u5e93\u62a5\u9519。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.publish</code>。
     */
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

    /**
     * stub操作者处理入口。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>。
     *
     * @param role 角色；类型为 <code>String</code>
     */
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
    /**
     * captured事件处理入口。
     * 关键处理步骤调用 <code>org.mockito.ArgumentCaptor.forClass</code>、<code>captor.capture</code>、<code>captor.getValue</code>。
     * @return 类型为 <code>Object</code> 的处理结果
     */
    private Object capturedEvent() {
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    /**
     * 当前操作对应的身份对象处理入口。
     * 关键处理步骤调用 <code>CurrentActorApi.CurrentActor.builder</code>、<code>Set.of</code>。
     *
     * @param role 角色；类型为 <code>String</code>
     * @return 类型为 <code>CurrentActorApi.CurrentActor</code> 的处理结果
     */
    private static CurrentActorApi.CurrentActor actor(String role) {
        return CurrentActorApi.CurrentActor.builder()
                .accountId(1L)
                .roles(Set.of(role))
                .permissions(Set.of("blog:publish", "blog:withdraw", "blog:edit"))
                .build();
    }

    /**
     * 文章处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setStatus</code>、<code>entity.setTitle</code>、<code>entity.setSlug</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param title 标题；类型为 <code>String</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param summary 摘要；类型为 <code>String</code>
     * @param body 正文；类型为 <code>String</code>
     * @param coverMediaAssetId 封面媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
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

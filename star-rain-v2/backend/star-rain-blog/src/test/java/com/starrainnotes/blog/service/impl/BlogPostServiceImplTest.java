package com.starrainnotes.blog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.blog.dto.BlogPostBodyDTO;
import com.starrainnotes.blog.dto.BlogPostCreateDTO;
import com.starrainnotes.blog.dto.BlogPostQueryDTO;
import com.starrainnotes.blog.dto.BlogPostUpdateDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.constant.MediaUsageCodes;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * BLOG-003 文章编辑测试。
 *
 * 重点在三处容易出错的地方：
 * 1. 封面替换的引用顺序（先 attach 新、再 detach 旧）；
 * 2. 正文保存时 blog.content 引用整体重建，且封面引用必须被重新登记；
 * 3. 删除文章时媒体引用必须解除，否则 Media 侧会永远拒绝归档那些文件。
 */
@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {

    @Mock
    private BlogPostMapper postMapper;

    @Mock
    private BlogTagMapper tagMapper;

    @Mock
    private BlogTopicMapper topicMapper;

    @Mock
    private MediaReferenceApi mediaReferenceApi;

    @Mock
    private CurrentActorApi currentActorApi;

    @Mock
    private BlogViewAssembler assembler;

    @InjectMocks
    private BlogPostServiceImpl service;

    /*
     * 当前主体用 lenient 打桩：查询与参数校验类用例根本不会走到“取当前账户”，
     * 严格模式下未使用的打桩会直接让测试失败，而这里的打桩只是为了让写操作用例少写一行。
     */
    @BeforeEach
    void actor() {
        lenient().when(currentActorApi.current()).thenReturn(CurrentActorApi.CurrentActor.builder()
                .accountId(1L)
                .roles(Set.of("SUPER_ADMIN"))
                .permissions(Set.of("blog:edit"))
                .build());
    }

    // ------------------------------------------------------------------
    // 创建
    // ------------------------------------------------------------------

    @Test
    @DisplayName("创建草稿：slug 规范化、状态固定 DRAFT、作者取自当前主体")
    void createNormalizesSlugAndFixesStatus() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("  First-Post  ");
        request.setTitle("  第一篇  ");
        request.setSummary("  摘要  ");
        when(postMapper.countBySlug("first-post", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(postMapper.postById(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).slug("first-post").build());

        BlogPostAdminVO result = service.create(request);

        ArgumentCaptor<BlogPostEntity> captor = ArgumentCaptor.forClass(BlogPostEntity.class);
        verify(postMapper).insertPost(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("first-post");
        assertThat(captor.getValue().getTitle()).isEqualTo("第一篇");
        assertThat(captor.getValue().getSummary()).isEqualTo("摘要");
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getCreatedByAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getBodyMarkdown()).isEmpty();
        assertThat(result.getId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("slug 冲突报 BLOG_POST_SLUG_CONFLICT")
    void createRejectsDuplicateSlug() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("第一篇");
        when(postMapper.countBySlug("first-post", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_SLUG_CONFLICT");
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    @DisplayName("slug 格式非法报 BLOG_POST_SLUG_INVALID")
    void createRejectsInvalidSlug() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("Bad Slug");
        request.setTitle("第一篇");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_SLUG_INVALID");
    }

    @Test
    @DisplayName("标题为空报 BLOG_POST_TITLE_INVALID")
    void createRejectsEmptyTitle() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("   ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_TITLE_INVALID");
    }

    @Test
    @DisplayName("带封面创建时在同一事务里登记 blog.cover 引用")
    void createAttachesCoverReference() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("第一篇");
        request.setCoverMediaAssetId(10L);
        when(postMapper.countBySlug("first-post", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(postMapper.postById(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        service.create(request);

        MediaReferenceCommand command = capturedAttach();
        assertThat(command.getMediaAssetId()).isEqualTo(10L);
        assertThat(command.getSourceModule()).isEqualTo("BLOG");
        assertThat(command.getSourceType()).isEqualTo("POST");
        assertThat(command.getSourceId()).isEqualTo(9L);
        assertThat(command.getUsageCode()).isEqualTo(MediaUsageCodes.BLOG_COVER);
    }

    @Test
    @DisplayName("无封面创建时不产生任何媒体引用")
    void createWithoutCoverSkipsReference() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("第一篇");
        when(postMapper.countBySlug("first-post", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(postMapper.postById(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        service.create(request);

        verify(mediaReferenceApi, never()).attach(any());
    }

    @Test
    @DisplayName("绑定已停用标签报 BLOG_TAG_DISABLED，历史绑定则不受影响")
    void createRejectsDisabledTagBinding() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("第一篇");
        request.setTagIds(List.of(5L));
        when(postMapper.countBySlug("first-post", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(tagMapper.tagsByPostId(9L)).thenReturn(List.of());
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "DISABLED"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_DISABLED");
        verify(tagMapper, never()).insertPostTag(anyLong(), anyLong());
    }

    @Test
    @DisplayName("绑定不存在的标签报 BLOG_TAG_NOT_FOUND")
    void createRejectsMissingTag() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setSlug("first-post");
        request.setTitle("第一篇");
        request.setTagIds(List.of(404L));
        when(postMapper.countBySlug("first-post", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(tagMapper.tagsByPostId(9L)).thenReturn(List.of());
        when(tagMapper.tagByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_NOT_FOUND");
    }

    // ------------------------------------------------------------------
    // 修改元数据与封面
    // ------------------------------------------------------------------

    @Test
    @DisplayName("换封面：先登记新引用再解除旧引用，避免出现“有封面无引用”")
    void updateCoverAttachesNewBeforeDetachingOld() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setCoverMediaAssetId(20L);
        service.update(9L, request);

        verify(postMapper).updatePostCover(9L, 20L, 1L);
        ArgumentCaptor<MediaReferenceCommand> attached = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi).attach(attached.capture());
        assertThat(attached.getValue().getMediaAssetId()).isEqualTo(20L);
        assertThat(attached.getValue().getUsageCode()).isEqualTo(MediaUsageCodes.BLOG_COVER);

        ArgumentCaptor<MediaReferenceCommand> detached = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi).detach(detached.capture());
        assertThat(detached.getValue().getMediaAssetId()).isEqualTo(10L);
        assertThat(detached.getValue().getUsageCode()).isEqualTo(MediaUsageCodes.BLOG_COVER);
    }

    @Test
    @DisplayName("取消封面：只解除旧引用，不登记新引用")
    void updateClearCover() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setClearCover(true);
        service.update(9L, request);

        verify(postMapper).updatePostCover(9L, null, 1L);
        verify(mediaReferenceApi).detach(any());
        verify(mediaReferenceApi, never()).attach(any());
    }

    @Test
    @DisplayName("未提供封面字段时不动封面，也不碰引用：PATCH 的 null 是“不修改”")
    void updateKeepsCoverWhenFieldMissing() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setTitle("改个标题");
        service.update(9L, request);

        verify(postMapper, never()).updatePostCover(anyLong(), any(), anyLong());
        verify(mediaReferenceApi, never()).attach(any());
        verify(mediaReferenceApi, never()).detach(any());
        verify(postMapper).updatePostMeta(9L, "first-post", "改个标题", "摘要", 1L);
    }

    @Test
    @DisplayName("已是封面的媒体重复提交不产生引用变更")
    void updateWithSameCoverSkipsReferenceWork() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setCoverMediaAssetId(10L);
        service.update(9L, request);

        verify(mediaReferenceApi, never()).attach(any());
        verify(mediaReferenceApi, never()).detach(any());
    }

    @Test
    @DisplayName("已绑定的停用标签继续保留：停用不删除历史关系")
    void updateKeepsExistingDisabledTag() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());
        when(tagMapper.tagsByPostId(9L)).thenReturn(List.of(BlogTagVO.builder().id(5L).status("DISABLED").build()));

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setTagIds(List.of(5L));
        service.update(9L, request);

        verify(tagMapper, never()).insertPostTag(anyLong(), anyLong());
        verify(tagMapper, never()).deletePostTag(anyLong(), anyLong());
    }

    @Test
    @DisplayName("标签解绑：不在目标集合里的绑定被删除")
    void updateRemovesTagsNotInTarget() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());
        when(tagMapper.tagsByPostId(9L)).thenReturn(List.of(
                BlogTagVO.builder().id(5L).status("ENABLED").build(),
                BlogTagVO.builder().id(6L).status("ENABLED").build()));

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setTagIds(List.of(6L));
        service.update(9L, request);

        verify(tagMapper).deletePostTag(9L, 5L);
        verify(tagMapper, never()).deletePostTag(9L, 6L);
    }

    @Test
    @DisplayName("改 slug 时排除自己，不算冲突")
    void updateSlugExcludesSelf() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).build());
        when(postMapper.countBySlug("second-post", 9L)).thenReturn(0L);

        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setSlug("second-post");
        service.update(9L, request);

        verify(postMapper).countBySlug("second-post", 9L);
        verify(postMapper).updatePostMeta(9L, "second-post", "第一篇", "摘要", 1L);
    }

    // ------------------------------------------------------------------
    // 正文与媒体引用
    // ------------------------------------------------------------------

    @Test
    @DisplayName("保存正文：解除全部引用后按正文重建 blog.content，并补回 blog.cover")
    void updateBodyRebuildsContentReferences() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminDetailVO(any())).thenReturn(null);

        BlogPostBodyDTO request = new BlogPostBodyDTO();
        request.setBodyMarkdown("开场\n\n![](/api/media/assets/42/content)\n![](/api/media/assets/43/content)\n");
        service.updateBody(9L, request);

        verify(postMapper).updatePostBody(9L, request.getBodyMarkdown(), 1L);
        verify(mediaReferenceApi).detachAll("BLOG", "POST", 9L);

        ArgumentCaptor<MediaReferenceCommand> captor = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi, times(3)).attach(captor.capture());
        assertThat(captor.getAllValues()).extracting(MediaReferenceCommand::getMediaAssetId)
                .containsExactly(10L, 42L, 43L);
        assertThat(captor.getAllValues()).extracting(MediaReferenceCommand::getUsageCode)
                .containsExactly(MediaUsageCodes.BLOG_COVER,
                        MediaUsageCodes.BLOG_CONTENT, MediaUsageCodes.BLOG_CONTENT);
    }

    @Test
    @DisplayName("空正文只解除引用，不登记任何内容引用（封面仍保留）")
    void updateBodyWithEmptyContentDetachesOnly() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        existing.setCoverMediaAssetId(10L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminDetailVO(any())).thenReturn(null);

        BlogPostBodyDTO request = new BlogPostBodyDTO();
        request.setBodyMarkdown("");
        service.updateBody(9L, request);

        verify(mediaReferenceApi).detachAll("BLOG", "POST", 9L);
        verify(mediaReferenceApi).attach(any());
        ArgumentCaptor<MediaReferenceCommand> captor = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi).attach(captor.capture());
        assertThat(captor.getValue().getUsageCode()).isEqualTo(MediaUsageCodes.BLOG_COVER);
    }

    @Test
    @DisplayName("正文为 null 视为清空正文，不抛异常")
    void updateBodyWithNullMarkdown() {
        BlogPostEntity existing = post(9L, "DRAFT", 1L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        when(assembler.toAdminDetailVO(any())).thenReturn(null);

        BlogPostBodyDTO request = new BlogPostBodyDTO();
        service.updateBody(9L, request);

        verify(postMapper).updatePostBody(9L, "", 1L);
        verify(mediaReferenceApi).detachAll("BLOG", "POST", 9L);
        verify(mediaReferenceApi, never()).attach(any());
    }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    @Test
    @DisplayName("删除草稿：清理标签、专题关系并解除媒体引用")
    void deleteRemovesRelationsAndMediaReferences() {
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(topicMapper.topicsByPostId(9L)).thenReturn(List.of());
        when(postMapper.deletePost(9L)).thenReturn(1);

        service.delete(9L);

        verify(tagMapper).deletePostTagsByPostId(9L);
        verify(topicMapper).deleteTopicPostsByPostId(9L);
        verify(mediaReferenceApi).detachAll("BLOG", "POST", 9L);
        verify(postMapper).deletePost(9L);
    }

    @Test
    @DisplayName("删除后压缩受影响专题的序号，保持 1..n 连续")
    void deleteCompactsAffectedTopicOrder() {
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(topicMapper.topicsByPostId(9L)).thenReturn(List.of(BlogTopicVO.builder().id(3L).build()));
        when(postMapper.deletePost(9L)).thenReturn(1);
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(
                BlogTopicMemberRow.builder().postId(20L).sortOrder(2).build(),
                BlogTopicMemberRow.builder().postId(30L).sortOrder(3).build()));

        service.delete(9L);

        verify(topicMapper).updateTopicOrder(eq(3L), argThat(items -> items.size() == 2
                && items.get(0).getPostId().equals(20L) && items.get(0).getSortOrder() == 1
                && items.get(1).getPostId().equals(30L) && items.get(1).getSortOrder() == 2));
    }

    @Test
    @DisplayName("已发布文章不能直接删除：必须先撤回，避免公开 URL 突然 404")
    void deleteRejectsPublishedPost() {
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "PUBLISHED", 1L));

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
        verify(mediaReferenceApi, never()).detachAll(any(), any(), anyLong());
        verify(postMapper, never()).deletePost(anyLong());
    }

    @Test
    @DisplayName("已撤回文章可以删除，引用同样被解除")
    void deleteWithdrawnPost() {
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "WITHDRAWN", 1L));
        when(topicMapper.topicsByPostId(9L)).thenReturn(List.of());
        when(postMapper.deletePost(9L)).thenReturn(1);

        service.delete(9L);

        verify(mediaReferenceApi).detachAll("BLOG", "POST", 9L);
        verify(postMapper).deletePost(9L);
    }

    @Test
    @DisplayName("条件删除影响 0 行时报状态错误，而不是假装删掉了")
    void deleteReportsConflictWhenConditionalDeleteMisses() {
        when(postMapper.postByIdForUpdate(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(topicMapper.topicsByPostId(9L)).thenReturn(List.of());
        when(postMapper.deletePost(9L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_STATE_INVALID");
    }

    // ------------------------------------------------------------------
    // 查询与详情
    // ------------------------------------------------------------------

    @Test
    @DisplayName("列表分页越界报 BLOG_QUERY_INVALID，不落到数据库报错")
    void pageRejectsInvalidPageSize() {
        BlogPostQueryDTO query = new BlogPostQueryDTO();
        query.setPageSize(500);

        assertThatThrownBy(() -> service.page(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
        verify(postMapper, never()).adminPageCount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("列表筛选值规范化后传给持久层：大小写不敏感、空白视为不筛选")
    void pageNormalizesFilters() {
        BlogPostQueryDTO query = new BlogPostQueryDTO();
        query.setPage(2);
        query.setPageSize(10);
        query.setStatus("  draft ");
        query.setKeyword("  Kafka ");
        when(postMapper.adminPageCount("kafka", "DRAFT", null, null)).thenReturn(1L);
        when(postMapper.adminPage("kafka", "DRAFT", null, null, 10, 10)).thenReturn(List.of(post(9L, "DRAFT", 1L)));
        when(assembler.toAdminVOs(any())).thenReturn(List.of(BlogPostAdminVO.builder().id(9L).build()));

        var result = service.page(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("列表筛选值非法报 BLOG_QUERY_INVALID，不静默忽略")
    void pageRejectsInvalidStatus() {
        BlogPostQueryDTO query = new BlogPostQueryDTO();
        query.setStatus("DELETED");

        assertThatThrownBy(() -> service.page(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("文章不存在时详情报 BLOG_POST_NOT_FOUND")
    void detailNotFound() {
        when(postMapper.postById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");
    }

    @Test
    @DisplayName("预览与详情读到同一份数据")
    void previewUsesSameSourceAsDetail() {
        BlogPostEntity post = post(9L, "DRAFT", 1L);
        when(postMapper.postById(9L)).thenReturn(post);
        when(assembler.toAdminDetailVO(post)).thenReturn(null);

        service.preview(9L);
        service.detail(9L);

        verify(assembler, times(2)).toAdminDetailVO(post);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private MediaReferenceCommand capturedAttach() {
        ArgumentCaptor<MediaReferenceCommand> captor = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi).attach(captor.capture());
        return captor.getValue();
    }

    private static BlogPostEntity post(Long id, String status, Long actorId) {
        BlogPostEntity entity = new BlogPostEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setTitle("第一篇");
        entity.setSlug("first-post");
        entity.setSummary("摘要");
        entity.setBodyMarkdown("正文");
        entity.setCreatedByAccountId(actorId);
        entity.setUpdatedByAccountId(actorId);
        return entity;
    }

    private static BlogTagEntity tag(Long id, String status) {
        BlogTagEntity entity = new BlogTagEntity();
        entity.setId(id);
        entity.setSlug("tag-" + id);
        entity.setName("标签" + id);
        entity.setStatus(status);
        return entity;
    }
}

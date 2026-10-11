/*
 * BlogPostServiceImplTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、BlogPostBodyDTO、BlogPostCreateDTO、BlogPostQueryDTO、BlogPostUpdateDTO、BlogTopicMemberRow、BlogPostEntity。
 * 阅读入口：actor（当前操作对应的身份对象处理入口）；createNormalizesSlugAndFixesStatus（\u521b\u5efa\u8349\u7a3f\uff1aslug \u89c4\u8303\u5316\u3001\u72b6\u6001\u56fa\u5b9a DRAFT\u3001\u4f5c\u8005\u53d6\u81ea\u5f53\u524d\u4e3b\u4f53）；createRejectsDuplicateSlug（slug \u51b2\u7a81\u62a5 BLOG_POST_SLUG_CONFLICT）；createDerivesChineseSlugAndResolvesExistingCollision（\u4e0d\u586b slug \u65f6\u4ece\u4e2d\u6587\u6807\u9898\u6d3e\u751f\uff0c\u91cd\u590d\u6807\u9898\u4f7f\u7528\u9012\u589e\u540e\u7f00）。
 */

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
import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogPostSlugDeriver;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/*
 * BLOG-003 文章编辑测试。
 *
 * 重点在三处容易出错的地方：
 * 1. 封面替换的引用顺序（先 attach 新、再 detach 旧）；
 * 2. 正文保存时 blog.content 引用整体重建，且封面引用必须被重新登记；
 * 3. 删除文章时媒体引用必须解除，否则 Media 侧会永远拒绝归档那些文件。
 */
/**
 * 博客文章业务服务实现测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogPostMapper postMapper;

    /**
     * 标签数据映射。
     * 协作类型：<code>BlogTagMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogTagMapper tagMapper;

    /**
     * 专题数据映射。
     * 协作类型：<code>BlogTopicMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogTopicMapper topicMapper;

    /**
     * 媒体引用接口。
     * 协作类型：<code>MediaReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaReferenceApi mediaReferenceApi;

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private CurrentActorApi currentActorApi;

    /**
     * 组装器。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogViewAssembler assembler;

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
    private BlogPostServiceImpl service;

    /*
     * 当前主体用 lenient 打桩：查询与参数校验类用例根本不会走到“取当前账户”，
     * 严格模式下未使用的打桩会直接让测试失败，而这里的打桩只是为了让写操作用例少写一行。
     */
    /**
     * 当前操作对应的身份对象处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
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

    /**
     * \u521b\u5efa\u8349\u7a3f\uff1aslug \u89c4\u8303\u5316\u3001\u72b6\u6001\u56fa\u5b9a DRAFT\u3001\u4f5c\u8005\u53d6\u81ea\u5f53\u524d\u4e3b\u4f53。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.create</code>。
     */
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

    /**
     * slug \u51b2\u7a81\u62a5 BLOG_POST_SLUG_CONFLICT。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>service.create</code>。
     */
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

    /**
     * \u4e0d\u586b slug \u65f6\u4ece\u4e2d\u6587\u6807\u9898\u6d3e\u751f\uff0c\u91cd\u590d\u6807\u9898\u4f7f\u7528\u9012\u589e\u540e\u7f00。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.create</code>。
     */
    @Test
    @DisplayName("不填 slug 时从中文标题派生，重复标题使用递增后缀")
    void createDerivesChineseSlugAndResolvesExistingCollision() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setTitle("纯中文标题");
        String base = BlogPostSlugDeriver.derive(request.getTitle());
        when(postMapper.countBySlug(base, null)).thenReturn(1L);
        when(postMapper.countBySlug(base + "-2", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(postMapper.postById(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).slug(base + "-2").build());

        BlogPostAdminVO created = service.create(request);

        assertThat(created.getSlug()).isEqualTo(base + "-2");
        verify(postMapper).insertPost(argThat(post -> (base + "-2").equals(post.getSlug())));
    }

    /**
     * \u9884\u68c0\u540e\u88ab\u5e76\u53d1\u521b\u5efa\u62a2\u5360 slug \u65f6\u91cd\u8bd5\uff0c\u4e0d\u80fd\u56de 500。
     * 主要协作调用：<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.create</code>。
     * 实现中的显式失败类型包括 <code>DuplicateKeyException</code>；具体触发条件见对应分支。
     */
    @Test
    @DisplayName("预检后被并发创建抢占 slug 时重试，不能回 500")
    void createRetriesDerivedSlugAfterUniqueKeyRace() {
        BlogPostCreateDTO request = new BlogPostCreateDTO();
        request.setTitle("Race Test");
        final int[] attempts = {0};
        doAnswer(invocation -> {
            if (attempts[0]++ == 0) {
                throw new DuplicateKeyException("simulated slug race");
            }
            invocation.<BlogPostEntity>getArgument(0).setId(9L);
            return null;
        }).when(postMapper).insertPost(any());
        when(postMapper.postById(9L)).thenReturn(post(9L, "DRAFT", 1L));
        when(assembler.toAdminVO(any())).thenReturn(BlogPostAdminVO.builder().id(9L).slug("race-test-2").build());

        BlogPostAdminVO created = service.create(request);

        assertThat(created.getSlug()).isEqualTo("race-test-2");
        verify(postMapper, times(2)).insertPost(any());
    }

    /**
     * slug \u683c\u5f0f\u975e\u6cd5\u62a5 BLOG_POST_SLUG_INVALID。
     * 主要协作调用：<code>service.create</code>。
     */
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

    /**
     * \u6807\u9898\u4e3a\u7a7a\u62a5 BLOG_POST_TITLE_INVALID。
     * 主要协作调用：<code>service.create</code>。
     */
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

    /**
     * \u5e26\u5c01\u9762\u521b\u5efa\u65f6\u5728\u540c\u4e00\u4e8b\u52a1\u91cc\u767b\u8bb0 blog.cover \u5f15\u7528。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.create</code>。
     */
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

    /**
     * \u65e0\u5c01\u9762\u521b\u5efa\u65f6\u4e0d\u4ea7\u751f\u4efb\u4f55\u5a92\u4f53\u5f15\u7528。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.create</code>。
     */
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

    /**
     * \u7ed1\u5b9a\u5df2\u505c\u7528\u6807\u7b7e\u62a5 BLOG_TAG_DISABLED\uff0c\u5386\u53f2\u7ed1\u5b9a\u5219\u4e0d\u53d7\u5f71\u54cd。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>tagMapper.tagsByPostId</code>、<code>tagMapper.tagByIdForUpdate</code>、<code>service.create</code>。
     */
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

    /**
     * \u7ed1\u5b9a\u4e0d\u5b58\u5728\u7684\u6807\u7b7e\u62a5 BLOG_TAG_NOT_FOUND。
     * 主要协作调用：<code>postMapper.countBySlug</code>、<code>tagMapper.tagsByPostId</code>、<code>tagMapper.tagByIdForUpdate</code>、<code>service.create</code>。
     */
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

    /**
     * \u6362\u5c01\u9762\uff1a\u5148\u767b\u8bb0\u65b0\u5f15\u7528\u518d\u89e3\u9664\u65e7\u5f15\u7528\uff0c\u907f\u514d\u51fa\u73b0\u201c\u6709\u5c01\u9762\u65e0\u5f15\u7528\u201d。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.update</code>。
     */
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

    /**
     * \u53d6\u6d88\u5c01\u9762\uff1a\u53ea\u89e3\u9664\u65e7\u5f15\u7528\uff0c\u4e0d\u767b\u8bb0\u65b0\u5f15\u7528。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.update</code>。
     */
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

    /**
     * \u672a\u63d0\u4f9b\u5c01\u9762\u5b57\u6bb5\u65f6\u4e0d\u52a8\u5c01\u9762\uff0c\u4e5f\u4e0d\u78b0\u5f15\u7528\uff1aPATCH \u7684 null \u662f\u201c\u4e0d\u4fee\u6539\u201d。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.update</code>。
     */
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

    /**
     * editing已发布元数据RefreshesDerivedDocuments处理入口。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>service.update</code>。
     */
    @Test
    void editingPublishedMetadataRefreshesDerivedDocuments() {
        BlogPostEntity existing = post(9L, "PUBLISHED", 1L);
        when(postMapper.postByIdForUpdate(9L)).thenReturn(existing);
        when(postMapper.postById(9L)).thenReturn(existing);
        BlogPostUpdateDTO request = new BlogPostUpdateDTO();
        request.setTitle("更新标题");

        service.update(9L, request);

        verify(eventPublisher).publishEvent((Object) argThat((Object event) ->
                event instanceof BlogPostChangedEvent changed
                        && changed.postId().equals(9L)
                        && changed.previousSlug().equals("first-post")
                        && changed.slug().equals("first-post")));
    }

    /**
     * \u5df2\u662f\u5c01\u9762\u7684\u5a92\u4f53\u91cd\u590d\u63d0\u4ea4\u4e0d\u4ea7\u751f\u5f15\u7528\u53d8\u66f4。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>service.update</code>。
     */
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

    /**
     * \u5df2\u7ed1\u5b9a\u7684\u505c\u7528\u6807\u7b7e\u7ee7\u7eed\u4fdd\u7559\uff1a\u505c\u7528\u4e0d\u5220\u9664\u5386\u53f2\u5173\u7cfb。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>tagMapper.tagsByPostId</code>、<code>service.update</code>。
     */
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

    /**
     * \u6807\u7b7e\u89e3\u7ed1\uff1a\u4e0d\u5728\u76ee\u6807\u96c6\u5408\u91cc\u7684\u7ed1\u5b9a\u88ab\u5220\u9664。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>tagMapper.tagsByPostId</code>、<code>service.update</code>。
     */
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

    /**
     * \u6539 slug \u65f6\u6392\u9664\u81ea\u5df1\uff0c\u4e0d\u7b97\u51b2\u7a81。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminVO</code>、<code>postMapper.countBySlug</code>、<code>service.update</code>。
     */
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

    /**
     * \u4fdd\u5b58\u6b63\u6587\uff1a\u89e3\u9664\u5168\u90e8\u5f15\u7528\u540e\u6309\u6b63\u6587\u91cd\u5efa blog.content\uff0c\u5e76\u8865\u56de blog.cover。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminDetailVO</code>、<code>service.updateBody</code>。
     */
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

    /**
     * \u7a7a\u6b63\u6587\u53ea\u89e3\u9664\u5f15\u7528\uff0c\u4e0d\u767b\u8bb0\u4efb\u4f55\u5185\u5bb9\u5f15\u7528\uff08\u5c01\u9762\u4ecd\u4fdd\u7559\uff09。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminDetailVO</code>、<code>service.updateBody</code>。
     */
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

    /**
     * \u6b63\u6587\u4e3a null \u89c6\u4e3a\u6e05\u7a7a\u6b63\u6587\uff0c\u4e0d\u629b\u5f02\u5e38。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>postMapper.postById</code>、<code>assembler.toAdminDetailVO</code>、<code>service.updateBody</code>。
     */
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

    /**
     * \u5220\u9664\u8349\u7a3f\uff1a\u6e05\u7406\u6807\u7b7e\u3001\u4e13\u9898\u5173\u7cfb\u5e76\u89e3\u9664\u5a92\u4f53\u5f15\u7528。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>topicMapper.topicsByPostId</code>、<code>postMapper.deletePost</code>、<code>service.delete</code>。
     */
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

    /**
     * \u5220\u9664\u540e\u538b\u7f29\u53d7\u5f71\u54cd\u4e13\u9898\u7684\u5e8f\u53f7\uff0c\u4fdd\u6301 1..n \u8fde\u7eed。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>topicMapper.topicsByPostId</code>、<code>postMapper.deletePost</code>、<code>topicMapper.topicMembers</code>、<code>service.delete</code>。
     */
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

    /**
     * \u5df2\u53d1\u5e03\u6587\u7ae0\u4e0d\u80fd\u76f4\u63a5\u5220\u9664\uff1a\u5fc5\u987b\u5148\u64a4\u56de\uff0c\u907f\u514d\u516c\u5f00 URL \u7a81\u7136 404。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>service.delete</code>。
     */
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

    /**
     * \u5df2\u64a4\u56de\u6587\u7ae0\u53ef\u4ee5\u5220\u9664\uff0c\u5f15\u7528\u540c\u6837\u88ab\u89e3\u9664。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>topicMapper.topicsByPostId</code>、<code>postMapper.deletePost</code>、<code>service.delete</code>。
     */
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

    /**
     * \u6761\u4ef6\u5220\u9664\u5f71\u54cd 0 \u884c\u65f6\u62a5\u72b6\u6001\u9519\u8bef\uff0c\u800c\u4e0d\u662f\u5047\u88c5\u5220\u6389\u4e86。
     * 主要协作调用：<code>postMapper.postByIdForUpdate</code>、<code>topicMapper.topicsByPostId</code>、<code>postMapper.deletePost</code>、<code>service.delete</code>。
     */
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

    /**
     * \u5217\u8868\u5206\u9875\u8d8a\u754c\u62a5 BLOG_QUERY_INVALID\uff0c\u4e0d\u843d\u5230\u6570\u636e\u5e93\u62a5\u9519。
     * 主要协作调用：<code>service.page</code>。
     */
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

    /**
     * \u5217\u8868\u7b5b\u9009\u503c\u89c4\u8303\u5316\u540e\u4f20\u7ed9\u6301\u4e45\u5c42\uff1a\u5927\u5c0f\u5199\u4e0d\u654f\u611f\u3001\u7a7a\u767d\u89c6\u4e3a\u4e0d\u7b5b\u9009。
     * 主要协作调用：<code>postMapper.adminPageCount</code>、<code>postMapper.adminPage</code>、<code>assembler.toAdminVOs</code>、<code>service.page</code>。
     */
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

    /**
     * \u5217\u8868\u7b5b\u9009\u503c\u975e\u6cd5\u62a5 BLOG_QUERY_INVALID\uff0c\u4e0d\u9759\u9ed8\u5ffd\u7565。
     * 主要协作调用：<code>service.page</code>。
     */
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

    /**
     * \u6587\u7ae0\u4e0d\u5b58\u5728\u65f6\u8be6\u60c5\u62a5 BLOG_POST_NOT_FOUND。
     * 主要协作调用：<code>postMapper.postById</code>、<code>service.detail</code>。
     */
    @Test
    @DisplayName("文章不存在时详情报 BLOG_POST_NOT_FOUND")
    void detailNotFound() {
        when(postMapper.postById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");
    }

    /**
     * \u9884\u89c8\u4e0e\u8be6\u60c5\u8bfb\u5230\u540c\u4e00\u4efd\u6570\u636e。
     * 主要协作调用：<code>postMapper.postById</code>、<code>assembler.toAdminDetailVO</code>、<code>service.preview</code>、<code>service.detail</code>。
     */
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

    /**
     * capturedAttach处理入口。
     * 关键处理步骤调用 <code>ArgumentCaptor.forClass</code>、<code>captor.capture</code>、<code>captor.getValue</code>。
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private MediaReferenceCommand capturedAttach() {
        ArgumentCaptor<MediaReferenceCommand> captor = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(mediaReferenceApi).attach(captor.capture());
        return captor.getValue();
    }

    /**
     * 文章处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setStatus</code>、<code>entity.setTitle</code>、<code>entity.setSlug</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
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

    /**
     * 标签处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setSlug</code>、<code>entity.setName</code>、<code>entity.setStatus</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>BlogTagEntity</code> 的处理结果
     */
    private static BlogTagEntity tag(Long id, String status) {
        BlogTagEntity entity = new BlogTagEntity();
        entity.setId(id);
        entity.setSlug("tag-" + id);
        entity.setName("标签" + id);
        entity.setStatus(status);
        return entity;
    }
}

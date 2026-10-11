/*
 * BlogTopicServiceImplTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogTopicDTO、BlogTopicChangedEvent、BlogTopicMemberRow、BlogTopicQueryDTO、BlogPostEntity、BlogTopicEntity、BlogPostMapper。
 * 阅读入口：createAllowsDuplicateName（Topic \u540d\u5b57\u5141\u8bb8\u91cd\u590d\uff1a\u6301\u4e45\u5c42\u6839\u672c\u6ca1\u6709\u6309\u540d\u5b57\u67e5\u91cd\u7684\u65b9\u6cd5\uff08Tag \u6709\uff09）；createDerivesUniqueSlugForDuplicateTopicNames（\u540c\u540d\u4e13\u9898\u81ea\u52a8\u5206\u914d\u4e0d\u540c slug\uff0c\u4ecd\u5141\u8bb8\u76f8\u540c\u663e\u793a\u540d\u79f0）；createStartsAtTopAndKeepsFeaturedChoice（\u65b0\u4e13\u9898\u63d2\u5728\u771f\u5b9e\u6392\u5e8f\u9996\u4f4d\uff0c\u7cbe\u9009\u6807\u8bb0\u6309\u8868\u5355\u4fdd\u5b58）；reorderTopicsPersistsNavigationOrder（\u8c03\u6574\u4e13\u9898\u987a\u5e8f\u65f6\u4fdd\u5b58\u6bcf\u4e2a\u4e13\u9898\u7684\u65b0\u5e8f\u53f7\u548c\u7cbe\u9009\u6807\u8bb0）。
 */

package com.starrainnotes.blog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.blog.dto.BlogTopicDTO;
import com.starrainnotes.blog.api.event.BlogTopicChangedEvent;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import org.springframework.context.ApplicationEventPublisher;
import com.starrainnotes.blog.utils.BlogSlugDeriver;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.exception.ApiException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * BLOG-005 / BLOG-006 / BLOG-007 测试。
 *
 * 与 Tag 的对照关系是本测试的重点：
 * - Topic 名字允许重复（只保证 slug 唯一），因此创建时根本不会去查重名；
 * - Topic 有 sortOrder：加入追加到末尾、移出压缩序号、重排按整集合重写 1..n；
 * - Topic 停用只影响“能否加入新文章”，成员与顺序全部保留。
 */
/**
 * 博客专题业务服务实现测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class BlogTopicServiceImplTest {

    /**
     * 专题数据映射。
     * 协作类型：<code>BlogTopicMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogTopicMapper topicMapper;

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogPostMapper postMapper;

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
    private BlogTopicServiceImpl service;

    // ------------------------------------------------------------------
    // 创建与修改
    // ------------------------------------------------------------------

    /**
     * Topic \u540d\u5b57\u5141\u8bb8\u91cd\u590d\uff1a\u6301\u4e45\u5c42\u6839\u672c\u6ca1\u6709\u6309\u540d\u5b57\u67e5\u91cd\u7684\u65b9\u6cd5\uff08Tag \u6709\uff09。
     * 主要协作调用：<code>topicMapper.countBySlug</code>、<code>topicMapper.topicById</code>、<code>topicMapper.countTopicPosts</code>、<code>service.create</code>。
     */
    @Test
    @DisplayName("Topic 名字允许重复：持久层根本没有按名字查重的方法（Tag 有）")
    void createAllowsDuplicateName() {
        when(topicMapper.countBySlug("java-roadmap", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogTopicEntity>getArgument(0).setId(3L);
            return null;
        }).when(topicMapper).insertTopic(any());
        when(topicMapper.topicById(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.countTopicPosts(3L)).thenReturn(0L);

        BlogTopicVO created = service.create(dto("java-roadmap", "Java 学习路线"));

        assertThat(created.getId()).isEqualTo(3L);
        verify(eventPublisher).publishEvent((Object) argThat((Object event) ->
                event instanceof BlogTopicChangedEvent changed
                        && changed.topicId().equals(3L) && changed.visible()));
        // 结构性断言：Topic 没有 name 唯一约束，因此 Mapper 里不存在按名字查询的语句。
        // 这是 Tag（有 countByName）与 Topic 最直接的分界。
        assertThat(Arrays.stream(BlogTopicMapper.class.getDeclaredMethods()).map(Method::getName).toList())
                .noneMatch(name -> name.toLowerCase().contains("byname"));
        assertThat(Arrays.stream(com.starrainnotes.blog.mapper.BlogTagMapper.class.getDeclaredMethods())
                .map(Method::getName).toList())
                .anyMatch(name -> name.toLowerCase().contains("byname"));
    }

    /**
     * \u540c\u540d\u4e13\u9898\u81ea\u52a8\u5206\u914d\u4e0d\u540c slug\uff0c\u4ecd\u5141\u8bb8\u76f8\u540c\u663e\u793a\u540d\u79f0。
     * 主要协作调用：<code>topicMapper.countBySlug</code>、<code>topicMapper.topicById</code>、<code>service.create</code>。
     */
    @Test
    @DisplayName("同名专题自动分配不同 slug，仍允许相同显示名称")
    void createDerivesUniqueSlugForDuplicateTopicNames() {
        String base = BlogSlugDeriver.derive("学习路线", "topic", 120);
        when(topicMapper.countBySlug(base, null)).thenReturn(1L);
        doAnswer(invocation -> {
            invocation.<BlogTopicEntity>getArgument(0).setId(3L);
            return null;
        }).when(topicMapper).insertTopic(any());
        when(topicMapper.topicById(3L)).thenReturn(topic(3L, "ENABLED"));

        BlogTopicDTO request = new BlogTopicDTO();
        request.setName("学习路线");
        service.create(request);

        org.mockito.ArgumentCaptor<BlogTopicEntity> captor =
                org.mockito.ArgumentCaptor.forClass(BlogTopicEntity.class);
        verify(topicMapper).insertTopic(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo(base + "-2");
    }

    /**
     * \u65b0\u4e13\u9898\u63d2\u5728\u771f\u5b9e\u6392\u5e8f\u9996\u4f4d\uff0c\u7cbe\u9009\u6807\u8bb0\u6309\u8868\u5355\u4fdd\u5b58。
     * 主要协作调用：<code>topicMapper.minimumSortOrder</code>、<code>topicMapper.topicById</code>、<code>service.create</code>。
     */
    @Test
    @DisplayName("新专题插在真实排序首位，精选标记按表单保存")
    void createStartsAtTopAndKeepsFeaturedChoice() {
        when(topicMapper.minimumSortOrder()).thenReturn(20);
        doAnswer(invocation -> {
            invocation.<BlogTopicEntity>getArgument(0).setId(3L);
            return null;
        }).when(topicMapper).insertTopic(any());
        when(topicMapper.topicById(3L)).thenReturn(topic(3L, "ENABLED"));
        BlogTopicDTO request = dto("new-topic", "新专题");
        request.setFeatured(false);

        service.create(request);

        var captor = org.mockito.ArgumentCaptor.forClass(BlogTopicEntity.class);
        verify(topicMapper).insertTopic(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(10);
        assertThat(captor.getValue().getFeatured()).isFalse();
    }

    /**
     * \u8c03\u6574\u4e13\u9898\u987a\u5e8f\u65f6\u4fdd\u5b58\u6bcf\u4e2a\u4e13\u9898\u7684\u65b0\u5e8f\u53f7\u548c\u7cbe\u9009\u6807\u8bb0。
     * 主要协作调用：<code>topicMapper.adminPage</code>、<code>topicMapper.topicByIdForUpdate</code>、<code>service.reorderTopics</code>。
     */
    @Test
    @DisplayName("调整专题顺序时保存每个专题的新序号和精选标记")
    void reorderTopicsPersistsNavigationOrder() {
        when(topicMapper.adminPage(null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of(
                BlogTopicVO.builder().id(1L).build(), BlogTopicVO.builder().id(2L).build()));
        BlogTopicEntity first = topic(1L, "ENABLED");
        first.setFeatured(true);
        BlogTopicEntity second = topic(2L, "DISABLED");
        second.setFeatured(false);
        when(topicMapper.topicByIdForUpdate(2L)).thenReturn(second);
        when(topicMapper.topicByIdForUpdate(1L)).thenReturn(first);

        service.reorderTopics(List.of(2L, 1L));

        verify(topicMapper).updatePresentation(2L, 10, false);
        verify(topicMapper).updatePresentation(1L, 20, true);
    }

    /**
     * Topic slug \u552f\u4e00\uff1a\u91cd\u590d slug \u62a5 BLOG_TOPIC_SLUG_CONFLICT。
     * 主要协作调用：<code>topicMapper.countBySlug</code>、<code>service.create</code>。
     */
    @Test
    @DisplayName("Topic slug 唯一：重复 slug 报 BLOG_TOPIC_SLUG_CONFLICT")
    void createRejectsDuplicateSlug() {
        when(topicMapper.countBySlug("java-roadmap", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto("java-roadmap", "重复的专题")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_SLUG_CONFLICT");
        verify(topicMapper, never()).insertTopic(any());
    }

    /**
     * \u4e13\u9898 slug \u975e\u6cd5\u4e0e\u540d\u5b57\u4e3a\u7a7a\u5206\u522b\u62a5\u5404\u81ea\u7684\u9519\u8bef\u7801。
     * 主要协作调用：<code>service.create</code>。
     */
    @Test
    @DisplayName("专题 slug 非法与名字为空分别报各自的错误码")
    void createValidatesSlugAndName() {
        assertThatThrownBy(() -> service.create(dto("Bad Slug", "专题")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_SLUG_INVALID");
        assertThatThrownBy(() -> service.create(dto("java-roadmap", " ")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NAME_INVALID");
    }

    // ------------------------------------------------------------------
    // BLOG-006 加入 / 移出
    // ------------------------------------------------------------------

    /**
     * \u52a0\u5165\u4e13\u9898\u8ffd\u52a0\u5230\u672b\u5c3e\uff1a\u5e8f\u53f7 = \u5f53\u524d\u6700\u5927\u503c + 1。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>postMapper.postById</code>、<code>topicMapper.countTopicPost</code>、<code>topicMapper.maxSortOrder</code>、<code>service.addPost</code>。
     */
    @Test
    @DisplayName("加入专题追加到末尾：序号 = 当前最大值 + 1")
    void addPostAppendsToEnd() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(postMapper.postById(9L)).thenReturn(post(9L));
        when(topicMapper.countTopicPost(3L, 9L)).thenReturn(0L);
        when(topicMapper.maxSortOrder(3L)).thenReturn(4);

        service.addPost(3L, 9L);

        verify(topicMapper).insertTopicPost(3L, 9L, 5);
    }

    /**
     * \u7a7a\u4e13\u9898\u7684\u7b2c\u4e00\u4e2a\u6210\u5458\u5e8f\u53f7\u4ece 1 \u5f00\u59cb。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>postMapper.postById</code>、<code>topicMapper.countTopicPost</code>、<code>topicMapper.maxSortOrder</code>、<code>service.addPost</code>。
     */
    @Test
    @DisplayName("空专题的第一个成员序号从 1 开始")
    void addFirstPostStartsAtOne() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(postMapper.postById(9L)).thenReturn(post(9L));
        when(topicMapper.countTopicPost(3L, 9L)).thenReturn(0L);
        when(topicMapper.maxSortOrder(3L)).thenReturn(null);

        service.addPost(3L, 9L);

        verify(topicMapper).insertTopicPost(3L, 9L, 1);
    }

    /**
     * \u505c\u7528\u7684\u4e13\u9898\u4e0d\u80fd\u52a0\u5165\u65b0\u6587\u7ae0。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>service.addPost</code>。
     */
    @Test
    @DisplayName("停用的专题不能加入新文章")
    void addPostOnDisabledTopicRejected() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "DISABLED"));

        assertThatThrownBy(() -> service.addPost(3L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_DISABLED");
        verify(topicMapper, never()).insertTopicPost(anyLong(), anyLong(), anyInt());
    }

    /**
     * \u91cd\u590d\u52a0\u5165\u540c\u4e00\u6587\u7ae0\u62a5 BLOG_TOPIC_POST_EXISTS。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>postMapper.postById</code>、<code>topicMapper.countTopicPost</code>、<code>service.addPost</code>。
     */
    @Test
    @DisplayName("重复加入同一文章报 BLOG_TOPIC_POST_EXISTS")
    void addPostDuplicateRejected() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(postMapper.postById(9L)).thenReturn(post(9L));
        when(topicMapper.countTopicPost(3L, 9L)).thenReturn(1L);

        assertThatThrownBy(() -> service.addPost(3L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_POST_EXISTS");
    }

    /**
     * \u6587\u7ae0\u4e0d\u5b58\u5728\u62a5 BLOG_POST_NOT_FOUND\uff0c\u4e13\u9898\u4e0d\u5b58\u5728\u62a5 BLOG_TOPIC_NOT_FOUND。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>postMapper.postById</code>、<code>service.addPost</code>。
     */
    @Test
    @DisplayName("文章不存在报 BLOG_POST_NOT_FOUND，专题不存在报 BLOG_TOPIC_NOT_FOUND")
    void addPostValidatesExistence() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(postMapper.postById(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.addPost(3L, 404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");

        when(topicMapper.topicByIdForUpdate(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.addPost(404L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_FOUND");
    }

    /**
     * \u79fb\u51fa\u6210\u5458\u540e\u538b\u7f29\u5e8f\u53f7\uff0c\u4fdd\u6301 1..n \u8fde\u7eed。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.countTopicPost</code>、<code>topicMapper.topicMembers</code>、<code>service.removePost</code>。
     */
    @Test
    @DisplayName("移出成员后压缩序号，保持 1..n 连续")
    void removePostCompactsOrder() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.countTopicPost(3L, 9L)).thenReturn(1L);
        // 移除 9 之后剩下 20(原序号 2) 与 30(原序号 3)
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(
                member(20L, 2),
                member(30L, 3)));

        service.removePost(3L, 9L);

        verify(topicMapper).deleteTopicPost(3L, 9L);
        verify(topicMapper).updateTopicOrder(eq(3L), argThat(items -> items.size() == 2
                && items.get(0).getPostId().equals(20L) && items.get(0).getSortOrder() == 1
                && items.get(1).getPostId().equals(30L) && items.get(1).getSortOrder() == 2));
    }

    /**
     * \u79fb\u51fa\u4e0d\u662f\u6210\u5458\u7684\u6587\u7ae0\u62a5 BLOG_TOPIC_POST_NOT_FOUND。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.countTopicPost</code>、<code>service.removePost</code>。
     */
    @Test
    @DisplayName("移出不是成员的文章报 BLOG_TOPIC_POST_NOT_FOUND")
    void removeNonMemberRejected() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.countTopicPost(3L, 9L)).thenReturn(0L);

        assertThatThrownBy(() -> service.removePost(3L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_POST_NOT_FOUND");
    }

    // ------------------------------------------------------------------
    // BLOG-007 排序
    // ------------------------------------------------------------------

    /**
     * \u91cd\u6392\uff1a\u6309\u4f20\u5165\u987a\u5e8f\u628a\u5e8f\u53f7\u91cd\u5199\u4e3a 1..n。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.topicMembers</code>、<code>service.reorder</code>。
     */
    @Test
    @DisplayName("重排：按传入顺序把序号重写为 1..n")
    void reorderWritesSequentialOrder() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(
                member(11L, 1), member(12L, 2), member(13L, 3)));

        service.reorder(3L, List.of(13L, 11L, 12L));

        verify(topicMapper).updateTopicOrder(eq(3L), argThat(items -> items.size() == 3
                && items.get(0).getPostId().equals(13L) && items.get(0).getSortOrder() == 1
                && items.get(1).getPostId().equals(11L) && items.get(1).getSortOrder() == 2
                && items.get(2).getPostId().equals(12L) && items.get(2).getSortOrder() == 3));
    }

    /**
     * \u91cd\u6392\u96c6\u5408\u5fc5\u987b\u4e0e\u4e13\u9898\u6210\u5458\u5b8c\u5168\u4e00\u81f4\uff1a\u91cd\u590d\u3001\u7f3a\u5c11\u3001\u5916\u6765 ID \u5168\u90e8\u62d2\u7edd。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.topicMembers</code>、<code>service.reorder</code>。
     */
    @Test
    @DisplayName("重排集合必须与专题成员完全一致：重复、缺少、外来 ID 全部拒绝")
    void reorderRejectsInconsistentSets() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(
                member(11L, 1), member(12L, 2), member(13L, 3)));

        assertThatThrownBy(() -> service.reorder(3L, List.of(11L, 11L, 12L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        assertThatThrownBy(() -> service.reorder(3L, List.of(11L, 12L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        assertThatThrownBy(() -> service.reorder(3L, List.of(11L, 12L, 99L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_POST_NOT_FOUND");

        assertThatThrownBy(() -> service.reorder(3L, List.of()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        assertThatThrownBy(() -> service.reorder(3L, Arrays.asList(11L, 12L, null)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        verify(topicMapper, never()).updateTopicOrder(anyLong(), any());
    }

    /**
     * \u7a7a\u4e13\u9898\u91cd\u6392\u7a7a\u5217\u8868\u662f\u65e0\u64cd\u4f5c\uff0c\u4e0d\u7b97\u9519\u8bef。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.topicMembers</code>、<code>service.reorder</code>。
     */
    @Test
    @DisplayName("空专题重排空列表是无操作，不算错误")
    void reorderEmptyTopicIsNoop() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of());

        service.reorder(3L, List.of());

        verify(topicMapper, never()).updateTopicOrder(anyLong(), any());
    }

    /**
     * \u91cd\u6392\u4e0d\u5b58\u5728\u7684\u4e13\u9898\u62a5 BLOG_TOPIC_NOT_FOUND。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>service.reorder</code>。
     */
    @Test
    @DisplayName("重排不存在的专题报 BLOG_TOPIC_NOT_FOUND")
    void reorderMissingTopicRejected() {
        when(topicMapper.topicByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.reorder(404L, List.of(11L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_FOUND");
    }

    // ------------------------------------------------------------------
    // 停用 / 启用与成员读取
    // ------------------------------------------------------------------

    /**
     * \u505c\u7528\u4e0e\u542f\u7528\u4e13\u9898\u90fd\u4e0d\u6539\u52a8\u6210\u5458\u4e0e\u987a\u5e8f\uff0c\u91cd\u590d\u64cd\u4f5c\u5e42\u7b49。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>service.disable</code>、<code>service.enable</code>。
     */
    @Test
    @DisplayName("停用与启用专题都不改动成员与顺序，重复操作幂等")
    void disableAndEnableKeepMembersAndOrder() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        service.disable(3L);
        verify(topicMapper).updateStatus(3L, "DISABLED");
        verify(topicMapper, never()).deleteTopicPost(anyLong(), anyLong());
        verify(topicMapper, never()).updateTopicOrder(anyLong(), any());

        when(topicMapper.topicByIdForUpdate(4L)).thenReturn(topic(4L, "DISABLED"));
        service.enable(4L);
        verify(topicMapper).updateStatus(4L, "ENABLED");
        verify(topicMapper, never()).deleteTopicPost(anyLong(), anyLong());

        // 幂等：已经是目标状态时不再写库
        when(topicMapper.topicByIdForUpdate(5L)).thenReturn(topic(5L, "DISABLED"));
        service.disable(5L);
        verify(topicMapper, never()).updateStatus(5L, "DISABLED");
    }

    /**
     * \u4e13\u9898\u6210\u5458\u6309\u4eba\u5de5\u987a\u5e8f\u8fd4\u56de。
     * 主要协作调用：<code>topicMapper.topicById</code>、<code>topicMapper.topicMembers</code>、<code>service.members</code>。
     */
    @Test
    @DisplayName("专题成员按人工顺序返回")
    void membersReturnsOrderedRows() {
        when(topicMapper.topicById(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(member(13L, 1), member(11L, 2)));

        List<BlogTopicMemberRow> members = service.members(3L);

        assertThat(members).extracting(BlogTopicMemberRow::getPostId).containsExactly(13L, 11L);
        assertThat(members).extracting(BlogTopicMemberRow::getSortOrder).containsExactly(1, 2);
    }

    /**
     * \u8bfb\u53d6\u4e0d\u5b58\u5728\u4e13\u9898\u7684\u6210\u5458\u62a5 BLOG_TOPIC_NOT_FOUND。
     * 主要协作调用：<code>topicMapper.topicById</code>、<code>service.members</code>。
     */
    @Test
    @DisplayName("读取不存在专题的成员报 BLOG_TOPIC_NOT_FOUND")
    void membersOfMissingTopic() {
        when(topicMapper.topicById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.members(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_FOUND");
    }

    /**
     * \u5217\u8868\u5206\u9875\u4e0e\u72b6\u6001\u7b5b\u9009\u975e\u6cd5\u65f6\u62a5 BLOG_QUERY_INVALID。
     * 主要协作调用：<code>service.page</code>。
     */
    @Test
    @DisplayName("列表分页与状态筛选非法时报 BLOG_QUERY_INVALID")
    void pageValidatesParameters() {
        BlogTopicQueryDTO tooBig = new BlogTopicQueryDTO();
        tooBig.setPageSize(0);
        assertThatThrownBy(() -> service.page(tooBig))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        BlogTopicQueryDTO badStatus = new BlogTopicQueryDTO();
        badStatus.setStatus("DELETED");
        assertThatThrownBy(() -> service.page(badStatus))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    // ------------------------------------------------------------------
    // 删除（只对空专题开放）
    // ------------------------------------------------------------------

    /**
     * \u7a7a\u4e13\u9898\u53ef\u4ee5\u5220\u9664\uff1a\u53ea\u5220\u4e13\u9898\u672c\u8eab\uff0c\u4e0d\u52a8\u6210\u5458\u5173\u7cfb\u8868。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.countTopicPosts</code>、<code>service.delete</code>。
     */
    @Test
    @DisplayName("空专题可以删除：只删专题本身，不动成员关系表")
    void deleteRemovesEmptyTopic() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.countTopicPosts(3L)).thenReturn(0L);

        service.delete(3L);

        verify(topicMapper).deleteTopic(3L);
        // 空专题本来就没有关系行，级联删除不该出现（Mapper 也没有这个方法）
        verify(topicMapper, never()).deleteTopicPost(anyLong(), anyLong());
        verify(topicMapper, never()).deleteTopicPostsByPostId(anyLong());
    }

    /**
     * \u6709\u6210\u5458\u7684\u4e13\u9898\u62d2\u7edd\u5220\u9664\uff1a\u62a5 BLOG_TOPIC_NOT_EMPTY\uff0c\u4e14\u7edd\u4e0d\u843d\u5e93。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>topicMapper.countTopicPosts</code>、<code>service.delete</code>。
     */
    @Test
    @DisplayName("有成员的专题拒绝删除：报 BLOG_TOPIC_NOT_EMPTY，且绝不落库")
    void deleteRejectsTopicWithPosts() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.countTopicPosts(3L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_EMPTY");

        verify(topicMapper, never()).deleteTopic(anyLong());
    }

    /**
     * \u5220\u9664\u4e0d\u5b58\u5728\u7684\u4e13\u9898\u62a5 BLOG_TOPIC_NOT_FOUND。
     * 主要协作调用：<code>topicMapper.topicByIdForUpdate</code>、<code>service.delete</code>。
     */
    @Test
    @DisplayName("删除不存在的专题报 BLOG_TOPIC_NOT_FOUND")
    void deleteMissingTopic() {
        when(topicMapper.topicByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_FOUND");

        verify(topicMapper, never()).deleteTopic(anyLong());
    }

    /**
     * 传输数据对象处理入口。
     * 关键处理步骤调用 <code>dto.setSlug</code>、<code>dto.setName</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>BlogTopicDTO</code> 的处理结果
     */
    private static BlogTopicDTO dto(String slug, String name) {
        BlogTopicDTO dto = new BlogTopicDTO();
        dto.setSlug(slug);
        dto.setName(name);
        return dto;
    }

    /**
     * 专题处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setSlug</code>、<code>entity.setName</code>、<code>entity.setStatus</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>BlogTopicEntity</code> 的处理结果
     */
    private static BlogTopicEntity topic(Long id, String status) {
        BlogTopicEntity entity = new BlogTopicEntity();
        entity.setId(id);
        entity.setSlug("java-roadmap");
        entity.setName("Java 学习路线");
        entity.setStatus(status);
        return entity;
    }

    /**
     * 文章处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setSlug</code>、<code>entity.setTitle</code>、<code>entity.setStatus</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    private static BlogPostEntity post(Long id) {
        BlogPostEntity entity = new BlogPostEntity();
        entity.setId(id);
        entity.setSlug("first-post");
        entity.setTitle("第一篇");
        entity.setStatus("PUBLISHED");
        return entity;
    }

    /**
     * member处理入口。
     * 关键处理步骤调用 <code>BlogTopicMemberRow.builder</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>
     * @return 类型为 <code>BlogTopicMemberRow</code> 的处理结果
     */
    private static BlogTopicMemberRow member(Long postId, int sortOrder) {
        return BlogTopicMemberRow.builder().postId(postId).sortOrder(sortOrder).build();
    }
}

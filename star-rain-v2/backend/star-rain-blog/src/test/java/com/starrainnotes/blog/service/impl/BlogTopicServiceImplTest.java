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
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
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
@ExtendWith(MockitoExtension.class)
class BlogTopicServiceImplTest {

    @Mock
    private BlogTopicMapper topicMapper;

    @Mock
    private BlogPostMapper postMapper;

    @InjectMocks
    private BlogTopicServiceImpl service;

    // ------------------------------------------------------------------
    // 创建与修改
    // ------------------------------------------------------------------

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
        // 结构性断言：Topic 没有 name 唯一约束，因此 Mapper 里不存在按名字查询的语句。
        // 这是 Tag（有 countByName）与 Topic 最直接的分界。
        assertThat(Arrays.stream(BlogTopicMapper.class.getDeclaredMethods()).map(Method::getName).toList())
                .noneMatch(name -> name.toLowerCase().contains("byname"));
        assertThat(Arrays.stream(com.starrainnotes.blog.mapper.BlogTagMapper.class.getDeclaredMethods())
                .map(Method::getName).toList())
                .anyMatch(name -> name.toLowerCase().contains("byname"));
    }

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

    @Test
    @DisplayName("空专题重排空列表是无操作，不算错误")
    void reorderEmptyTopicIsNoop() {
        when(topicMapper.topicByIdForUpdate(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of());

        service.reorder(3L, List.of());

        verify(topicMapper, never()).updateTopicOrder(anyLong(), any());
    }

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

    @Test
    @DisplayName("专题成员按人工顺序返回")
    void membersReturnsOrderedRows() {
        when(topicMapper.topicById(3L)).thenReturn(topic(3L, "ENABLED"));
        when(topicMapper.topicMembers(3L)).thenReturn(List.of(member(13L, 1), member(11L, 2)));

        List<BlogTopicMemberRow> members = service.members(3L);

        assertThat(members).extracting(BlogTopicMemberRow::getPostId).containsExactly(13L, 11L);
        assertThat(members).extracting(BlogTopicMemberRow::getSortOrder).containsExactly(1, 2);
    }

    @Test
    @DisplayName("读取不存在专题的成员报 BLOG_TOPIC_NOT_FOUND")
    void membersOfMissingTopic() {
        when(topicMapper.topicById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.members(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TOPIC_NOT_FOUND");
    }

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

    private static BlogTopicDTO dto(String slug, String name) {
        BlogTopicDTO dto = new BlogTopicDTO();
        dto.setSlug(slug);
        dto.setName(name);
        return dto;
    }

    private static BlogTopicEntity topic(Long id, String status) {
        BlogTopicEntity entity = new BlogTopicEntity();
        entity.setId(id);
        entity.setSlug("java-roadmap");
        entity.setName("Java 学习路线");
        entity.setStatus(status);
        return entity;
    }

    private static BlogPostEntity post(Long id) {
        BlogPostEntity entity = new BlogPostEntity();
        entity.setId(id);
        entity.setSlug("first-post");
        entity.setTitle("第一篇");
        entity.setStatus("PUBLISHED");
        return entity;
    }

    private static BlogTopicMemberRow member(Long postId, int sortOrder) {
        return BlogTopicMemberRow.builder().postId(postId).sortOrder(sortOrder).build();
    }
}

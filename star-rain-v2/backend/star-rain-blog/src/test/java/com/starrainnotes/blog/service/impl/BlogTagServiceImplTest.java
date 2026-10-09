package com.starrainnotes.blog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.service.BlogTagService;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogSlugDeriver;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.exception.ApiException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * BLOG-004 标签管理测试。
 *
 * 这里固定 Tag 的语义：无序、名字唯一、只能停用不能删除。
 * 与之对照的 Topic 语义（名字可重复、有顺序）在 BlogTopicServiceImplTest 里验证。
 */
@ExtendWith(MockitoExtension.class)
class BlogTagServiceImplTest {

    @Mock
    private BlogTagMapper tagMapper;

    @Mock
    private BlogViewAssembler assembler;

    @InjectMocks
    private BlogTagServiceImpl service;

    @Test
    @DisplayName("Tag 名字唯一：同名标签报 BLOG_TAG_NAME_CONFLICT（Topic 则允许同名）")
    void createRejectsDuplicateName() {
        BlogTagDTO request = dto("java", "Java");
        when(tagMapper.countBySlug("java", null)).thenReturn(0L);
        when(tagMapper.countByName("Java", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_NAME_CONFLICT");
        verify(tagMapper, never()).insertTag(any());
    }

    @Test
    @DisplayName("Tag slug 唯一：重复 slug 报 BLOG_TAG_SLUG_CONFLICT")
    void createRejectsDuplicateSlug() {
        BlogTagDTO request = dto("java", "Java");
        when(tagMapper.countBySlug("java", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_SLUG_CONFLICT");
    }

    @Test
    @DisplayName("创建标签：slug 规范化、默认 ENABLED、没有任何排序字段")
    void createNormalizesAndDefaultsToEnabled() {
        when(tagMapper.countBySlug("java", null)).thenReturn(0L);
        when(tagMapper.countByName("Java", null)).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<BlogTagEntity>getArgument(0).setId(5L);
            return null;
        }).when(tagMapper).insertTag(any());
        when(tagMapper.tagById(5L)).thenReturn(tag(5L, "ENABLED"));
        when(assembler.toTagVO(any())).thenReturn(BlogTagVO.builder().id(5L).build());

        service.create(dto("  Java  ", "  Java  "));

        ArgumentCaptor<BlogTagEntity> captor = ArgumentCaptor.forClass(BlogTagEntity.class);
        verify(tagMapper).insertTag(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("java");
        assertThat(captor.getValue().getName()).isEqualTo("Java");
        assertThat(captor.getValue().getStatus()).isEqualTo("ENABLED");
    }

    @Test
    @DisplayName("只提交中文标签名称时自动生成 slug，冲突后追加序号")
    void createDerivesSlugFromNameAndRetriesCollision() {
        String base = BlogSlugDeriver.derive("学习笔记", "tag", 100);
        when(tagMapper.countBySlug(base, null)).thenReturn(1L);
        doAnswer(invocation -> {
            invocation.<BlogTagEntity>getArgument(0).setId(5L);
            return null;
        }).when(tagMapper).insertTag(any());
        when(tagMapper.tagById(5L)).thenReturn(tag(5L, "ENABLED"));
        when(assembler.toTagVO(any())).thenReturn(BlogTagVO.builder().id(5L).build());

        BlogTagDTO request = new BlogTagDTO();
        request.setName("学习笔记");
        service.create(request);

        ArgumentCaptor<BlogTagEntity> captor = ArgumentCaptor.forClass(BlogTagEntity.class);
        verify(tagMapper).insertTag(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo(base + "-2");
    }

    @Test
    @DisplayName("slug 非法与名字为空分别报各自的错误码")
    void createValidatesSlugAndName() {
        assertThatThrownBy(() -> service.create(dto("Bad Slug", "Java")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_SLUG_INVALID");
        assertThatThrownBy(() -> service.create(dto("java", "   ")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_NAME_INVALID");
    }

    @Test
    @DisplayName("说明超长报 BLOG_TAG_DESCRIPTION_INVALID，而不是等数据库报错")
    void createRejectsTooLongDescription() {
        BlogTagDTO request = dto("java", "Java");
        request.setDescription("说".repeat(501));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_DESCRIPTION_INVALID");
    }

    @Test
    @DisplayName("改名冲突时排除自己：改成原名不算冲突")
    void updateExcludesSelf() {
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "ENABLED"));
        when(tagMapper.tagById(5L)).thenReturn(tag(5L, "ENABLED"));
        when(assembler.toTagVO(any())).thenReturn(BlogTagVO.builder().id(5L).build());

        BlogTagDTO request = new BlogTagDTO();
        request.setDescription("新的说明");
        service.update(5L, request);

        verify(tagMapper, never()).countBySlug(anyString(), any());
        verify(tagMapper, never()).countByName(anyString(), any());
        verify(tagMapper).updateTag(5L, "java", "Java", "新的说明");
    }

    @Test
    @DisplayName("改 slug 撞上别的标签报 BLOG_TAG_SLUG_CONFLICT")
    void updateRejectsSlugConflict() {
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "ENABLED"));
        when(tagMapper.countBySlug("kotlin", 5L)).thenReturn(1L);

        BlogTagDTO request = new BlogTagDTO();
        request.setSlug("kotlin");

        assertThatThrownBy(() -> service.update(5L, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_SLUG_CONFLICT");
    }

    @Test
    @DisplayName("停用标签不删除任何历史绑定，重复停用幂等")
    void disableKeepsHistoryAndIsIdempotent() {
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "ENABLED"));

        service.disable(5L);

        verify(tagMapper).updateStatus(5L, "DISABLED");
        // “已绑定 Tag 禁用不会删除历史关系” —— 这两条断言就是这句话的可执行形式
        verify(tagMapper, never()).deletePostTag(anyLong(), anyLong());
        verify(tagMapper, never()).deletePostTagsByPostId(anyLong());

        when(tagMapper.tagByIdForUpdate(6L)).thenReturn(tag(6L, "DISABLED"));
        service.disable(6L);
        verify(tagMapper, never()).updateStatus(6L, "DISABLED");
    }

    @Test
    @DisplayName("重新启用把状态改回 ENABLED")
    void enableRestoresStatus() {
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "DISABLED"));

        service.enable(5L);

        verify(tagMapper).updateStatus(5L, "ENABLED");
    }

    @Test
    @DisplayName("标签不存在时停用报 BLOG_TAG_NOT_FOUND")
    void disableMissingTag() {
        when(tagMapper.tagByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.disable(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_NOT_FOUND");
    }

    @Test
    @DisplayName("列表分页与状态筛选非法时报 BLOG_QUERY_INVALID")
    void pageValidatesParameters() {
        BlogTagQueryDTO tooBig = new BlogTagQueryDTO();
        tooBig.setPageSize(500);
        assertThatThrownBy(() -> service.page(tooBig))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        BlogTagQueryDTO badStatus = new BlogTagQueryDTO();
        badStatus.setStatus("ARCHIVED");
        assertThatThrownBy(() -> service.page(badStatus))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("列表返回绑定数量：后台据此判断标签是否还在被使用")
    void pageReturnsPostCount() {
        BlogTagQueryDTO query = new BlogTagQueryDTO();
        when(tagMapper.adminPageCount(null, null)).thenReturn(1L);
        when(tagMapper.adminPage(null, null, 0, 20)).thenReturn(List.of(
                BlogTagVO.builder().id(5L).postCount(3L).build()));

        var result = service.page(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems().get(0).getPostCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("前台取标签走 publishedTags，只包含有已发布文章的标签")
    void listPublishedUsesPublishedQuery() {
        when(tagMapper.publishedTags()).thenReturn(List.of(BlogTagVO.builder().id(5L).build()));

        assertThat(service.listPublished()).hasSize(1);
    }

    @Test
    @DisplayName("服务契约不提供删除方法：V2 不允许级联删除已被文章使用的 Tag")
    void serviceExposesNoDeleteOperation() {
        List<String> methodNames = Arrays.stream(BlogTagService.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("delete"));
        assertThat(methodNames).contains("disable", "enable");
    }

    private static BlogTagDTO dto(String slug, String name) {
        BlogTagDTO dto = new BlogTagDTO();
        dto.setSlug(slug);
        dto.setName(name);
        return dto;
    }

    private static BlogTagEntity tag(Long id, String status) {
        BlogTagEntity entity = new BlogTagEntity();
        entity.setId(id);
        entity.setSlug("java");
        entity.setName("Java");
        entity.setStatus(status);
        return entity;
    }
}

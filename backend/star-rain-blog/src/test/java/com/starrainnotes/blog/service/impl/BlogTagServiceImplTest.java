/*
 * BlogTagServiceImplTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogTagDTO、BlogTagQueryDTO、BlogTagEntity、BlogTagMapper、BlogTagService、BlogViewAssembler、BlogSlugDeriver。
 * 阅读入口：createRejectsDuplicateName（Tag \u540d\u5b57\u552f\u4e00\uff1a\u540c\u540d\u6807\u7b7e\u62a5 BLOG_TAG_NAME_CONFLICT\uff08Topic \u5219\u5141\u8bb8\u540c\u540d\uff09）；createRejectsDuplicateSlug（Tag slug \u552f\u4e00\uff1a\u91cd\u590d slug \u62a5 BLOG_TAG_SLUG_CONFLICT）；createNormalizesAndDefaultsToEnabled（\u521b\u5efa\u6807\u7b7e\uff1aslug \u89c4\u8303\u5316\u3001\u9ed8\u8ba4 ENABLED\u3001\u6ca1\u6709\u4efb\u4f55\u6392\u5e8f\u5b57\u6bb5）；createDerivesSlugFromNameAndRetriesCollision（\u53ea\u63d0\u4ea4\u4e2d\u6587\u6807\u7b7e\u540d\u79f0\u65f6\u81ea\u52a8\u751f\u6210 slug\uff0c\u51b2\u7a81\u540e\u8ffd\u52a0\u5e8f\u53f7）。
 */

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
/**
 * 博客标签业务服务实现测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class BlogTagServiceImplTest {

    /**
     * 标签数据映射。
     * 协作类型：<code>BlogTagMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogTagMapper tagMapper;

    /**
     * 组装器。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private BlogViewAssembler assembler;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks
    private BlogTagServiceImpl service;

    /**
     * Tag \u540d\u5b57\u552f\u4e00\uff1a\u540c\u540d\u6807\u7b7e\u62a5 BLOG_TAG_NAME_CONFLICT\uff08Topic \u5219\u5141\u8bb8\u540c\u540d\uff09。
     * 主要协作调用：<code>tagMapper.countBySlug</code>、<code>tagMapper.countByName</code>、<code>service.create</code>。
     */
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

    /**
     * Tag slug \u552f\u4e00\uff1a\u91cd\u590d slug \u62a5 BLOG_TAG_SLUG_CONFLICT。
     * 主要协作调用：<code>tagMapper.countBySlug</code>、<code>service.create</code>。
     */
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

    /**
     * \u521b\u5efa\u6807\u7b7e\uff1aslug \u89c4\u8303\u5316\u3001\u9ed8\u8ba4 ENABLED\u3001\u6ca1\u6709\u4efb\u4f55\u6392\u5e8f\u5b57\u6bb5。
     * 主要协作调用：<code>tagMapper.countBySlug</code>、<code>tagMapper.countByName</code>、<code>tagMapper.tagById</code>、<code>assembler.toTagVO</code>、<code>service.create</code>。
     */
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

    /**
     * \u53ea\u63d0\u4ea4\u4e2d\u6587\u6807\u7b7e\u540d\u79f0\u65f6\u81ea\u52a8\u751f\u6210 slug\uff0c\u51b2\u7a81\u540e\u8ffd\u52a0\u5e8f\u53f7。
     * 主要协作调用：<code>tagMapper.countBySlug</code>、<code>tagMapper.tagById</code>、<code>assembler.toTagVO</code>、<code>service.create</code>。
     */
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

    /**
     * slug \u975e\u6cd5\u4e0e\u540d\u5b57\u4e3a\u7a7a\u5206\u522b\u62a5\u5404\u81ea\u7684\u9519\u8bef\u7801。
     * 主要协作调用：<code>service.create</code>。
     */
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

    /**
     * \u8bf4\u660e\u8d85\u957f\u62a5 BLOG_TAG_DESCRIPTION_INVALID\uff0c\u800c\u4e0d\u662f\u7b49\u6570\u636e\u5e93\u62a5\u9519。
     * 主要协作调用：<code>service.create</code>。
     */
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

    /**
     * \u6539\u540d\u51b2\u7a81\u65f6\u6392\u9664\u81ea\u5df1\uff1a\u6539\u6210\u539f\u540d\u4e0d\u7b97\u51b2\u7a81。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>、<code>tagMapper.tagById</code>、<code>assembler.toTagVO</code>、<code>service.update</code>。
     */
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

    /**
     * \u6539 slug \u649e\u4e0a\u522b\u7684\u6807\u7b7e\u62a5 BLOG_TAG_SLUG_CONFLICT。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>、<code>tagMapper.countBySlug</code>、<code>service.update</code>。
     */
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

    /**
     * \u505c\u7528\u6807\u7b7e\u4e0d\u5220\u9664\u4efb\u4f55\u5386\u53f2\u7ed1\u5b9a\uff0c\u91cd\u590d\u505c\u7528\u5e42\u7b49。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>、<code>service.disable</code>。
     */
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

    /**
     * \u91cd\u65b0\u542f\u7528\u628a\u72b6\u6001\u6539\u56de ENABLED。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>、<code>service.enable</code>。
     */
    @Test
    @DisplayName("重新启用把状态改回 ENABLED")
    void enableRestoresStatus() {
        when(tagMapper.tagByIdForUpdate(5L)).thenReturn(tag(5L, "DISABLED"));

        service.enable(5L);

        verify(tagMapper).updateStatus(5L, "ENABLED");
    }

    /**
     * \u6807\u7b7e\u4e0d\u5b58\u5728\u65f6\u505c\u7528\u62a5 BLOG_TAG_NOT_FOUND。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>、<code>service.disable</code>。
     */
    @Test
    @DisplayName("标签不存在时停用报 BLOG_TAG_NOT_FOUND")
    void disableMissingTag() {
        when(tagMapper.tagByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.disable(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_TAG_NOT_FOUND");
    }

    /**
     * \u5217\u8868\u5206\u9875\u4e0e\u72b6\u6001\u7b5b\u9009\u975e\u6cd5\u65f6\u62a5 BLOG_QUERY_INVALID。
     * 主要协作调用：<code>service.page</code>。
     */
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

    /**
     * \u5217\u8868\u8fd4\u56de\u7ed1\u5b9a\u6570\u91cf\uff1a\u540e\u53f0\u636e\u6b64\u5224\u65ad\u6807\u7b7e\u662f\u5426\u8fd8\u5728\u88ab\u4f7f\u7528。
     * 主要协作调用：<code>tagMapper.adminPageCount</code>、<code>tagMapper.adminPage</code>、<code>service.page</code>。
     */
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

    /**
     * \u524d\u53f0\u53d6\u6807\u7b7e\u8d70 publishedTags\uff0c\u53ea\u5305\u542b\u6709\u5df2\u53d1\u5e03\u6587\u7ae0\u7684\u6807\u7b7e。
     * 主要协作调用：<code>tagMapper.publishedTags</code>、<code>service.listPublished</code>。
     */
    @Test
    @DisplayName("前台取标签走 publishedTags，只包含有已发布文章的标签")
    void listPublishedUsesPublishedQuery() {
        when(tagMapper.publishedTags()).thenReturn(List.of(BlogTagVO.builder().id(5L).build()));

        assertThat(service.listPublished()).hasSize(1);
    }

    /**
     * \u670d\u52a1\u5951\u7ea6\u4e0d\u63d0\u4f9b\u5220\u9664\u65b9\u6cd5\uff1aV2 \u4e0d\u5141\u8bb8\u7ea7\u8054\u5220\u9664\u5df2\u88ab\u6587\u7ae0\u4f7f\u7528\u7684 Tag。
     * 关键处理步骤调用 <code>Arrays.stream</code>、<code>BlogTagService.class.getDeclaredMethods</code>、<code>name.toLowerCase</code>。
     */
    @Test
    @DisplayName("服务契约不提供删除方法：V2 不允许级联删除已被文章使用的 Tag")
    void serviceExposesNoDeleteOperation() {
        List<String> methodNames = Arrays.stream(BlogTagService.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("delete"));
        assertThat(methodNames).contains("disable", "enable");
    }

    /**
     * 传输数据对象处理入口。
     * 关键处理步骤调用 <code>dto.setSlug</code>、<code>dto.setName</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>BlogTagDTO</code> 的处理结果
     */
    private static BlogTagDTO dto(String slug, String name) {
        BlogTagDTO dto = new BlogTagDTO();
        dto.setSlug(slug);
        dto.setName(name);
        return dto;
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
        entity.setSlug("java");
        entity.setName("Java");
        entity.setStatus(status);
        return entity;
    }
}

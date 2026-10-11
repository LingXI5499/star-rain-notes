/*
 * BlogAdminTagController：博客模块的HTTP 接入文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：BlogPermissions、BlogTagDTO、BlogTagQueryDTO、BlogTagService、BlogTagVO、ApiResponse、PageResult。
 * 阅读入口：page（分页读取博客后台标签）；create（创建博客后台标签）；update（更新博客后台标签）；disable（禁用博客后台标签）。
 */

package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.constant.BlogPermissions;
import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.service.BlogTagService;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * BLOG-004 标签管理接口。
 *
 * 刻意没有 DELETE：V2 不提供级联删除已被文章使用的 Tag，
 * 而“未被使用才能删”的规则在个人博客里收益极低、误删代价极高，因此直接不给入口。
 * 需要下架就 disable。
 */
/**
 * 博客后台标签控制器，属于博客模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin/blog/tags")
public class BlogAdminTagController {

    /**
     * 标签业务服务。
     * 协作类型：<code>BlogTagService</code>。
     */
    private final BlogTagService tagService;

    /**
     * 构造BlogAdminTagController，接收本类使用的依赖或初始化参数。
     *
     * @param tagService 标签业务服务；类型为 <code>BlogTagService</code>
     */
    public BlogAdminTagController(BlogTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 分页读取博客后台标签。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>tagService.page</code>。
     *
     * @param query 查询参数；类型为 <code>BlogTagQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;BlogTagVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogTagVO>> page(BlogTagQueryDTO query) {
        return ApiResponse.ok(tagService.page(query));
    }

    /**
     * 创建博客后台标签。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>tagService.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogTagVO&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTagVO> create(@RequestBody BlogTagDTO request) {
        return ApiResponse.ok(tagService.create(request));
    }

    /**
     * 更新博客后台标签。
     * HTTP 映射：<code>@PatchMapping("/{tagId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>tagService.update</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogTagVO&gt;</code> 的处理结果
     */
    @PatchMapping("/{tagId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTagVO> update(@PathVariable Long tagId, @RequestBody BlogTagDTO request) {
        return ApiResponse.ok(tagService.update(tagId, request));
    }

    /**
     * 禁用博客后台标签。
     * HTTP 映射：<code>@PostMapping("/{tagId}/disable")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>tagService.disable</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{tagId}/disable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> disable(@PathVariable Long tagId) {
        tagService.disable(tagId);
        return ApiResponse.ok(null);
    }

    /**
     * 启用博客后台标签。
     * HTTP 映射：<code>@PostMapping("/{tagId}/enable")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>tagService.enable</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{tagId}/enable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> enable(@PathVariable Long tagId) {
        tagService.enable(tagId);
        return ApiResponse.ok(null);
    }
}

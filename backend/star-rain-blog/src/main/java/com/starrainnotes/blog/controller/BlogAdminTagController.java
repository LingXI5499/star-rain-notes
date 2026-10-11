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
@RestController
@RequestMapping("/api/admin/blog/tags")
public class BlogAdminTagController {

    private final BlogTagService tagService;

    public BlogAdminTagController(BlogTagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogTagVO>> page(BlogTagQueryDTO query) {
        return ApiResponse.ok(tagService.page(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTagVO> create(@RequestBody BlogTagDTO request) {
        return ApiResponse.ok(tagService.create(request));
    }

    @PatchMapping("/{tagId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTagVO> update(@PathVariable Long tagId, @RequestBody BlogTagDTO request) {
        return ApiResponse.ok(tagService.update(tagId, request));
    }

    @PostMapping("/{tagId}/disable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> disable(@PathVariable Long tagId) {
        tagService.disable(tagId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{tagId}/enable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> enable(@PathVariable Long tagId) {
        tagService.enable(tagId);
        return ApiResponse.ok(null);
    }
}

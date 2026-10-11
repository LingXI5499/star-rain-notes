package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.constant.BlogPermissions;
import com.starrainnotes.blog.dto.BlogPostBodyDTO;
import com.starrainnotes.blog.dto.BlogPostCreateDTO;
import com.starrainnotes.blog.dto.BlogPostQueryDTO;
import com.starrainnotes.blog.dto.BlogPostUpdateDTO;
import com.starrainnotes.blog.service.BlogPostService;
import com.starrainnotes.blog.service.BlogPublishService;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * BLOG-003 / BLOG-008 / BLOG-009 的后台接口。
 *
 * URL 层只要求“已认证”（见 BlogSecurityContributor），具体权限由这里的 @PreAuthorize 声明。
 * 读与写刻意分开权限码：只给“查看后台文章”的人不应该能改动内容。
 *
 * 发布类端点单独成一组（publish / withdraw / restore）：
 * 前端不能通过 PATCH status 字段完成发布，否则状态机与事件都会绕过。
 */
@RestController
@RequestMapping("/api/admin/blog/posts")
public class BlogAdminPostController {

    private final BlogPostService postService;
    private final BlogPublishService publishService;

    public BlogAdminPostController(BlogPostService postService, BlogPublishService publishService) {
        this.postService = postService;
        this.publishService = publishService;
    }

    // 后台列表：分页 + 关键词 + 状态 + 标签 + 专题
    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogPostAdminVO>> page(BlogPostQueryDTO query) {
        return ApiResponse.ok(postService.page(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminVO> create(@RequestBody BlogPostCreateDTO request) {
        return ApiResponse.ok(postService.create(request));
    }

    @GetMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<BlogPostAdminDetailVO> detail(@PathVariable Long postId) {
        return ApiResponse.ok(postService.detail(postId));
    }

    @PatchMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminVO> update(@PathVariable Long postId,
                                              @RequestBody BlogPostUpdateDTO request) {
        return ApiResponse.ok(postService.update(postId, request));
    }

    // 正文单独提交：避免每次小改标题都要重传整篇 Markdown
    @PutMapping("/{postId}/body")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminDetailVO> updateBody(@PathVariable Long postId,
                                                        @RequestBody BlogPostBodyDTO request) {
        return ApiResponse.ok(postService.updateBody(postId, request));
    }

    @GetMapping("/{postId}/preview")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<BlogPostAdminDetailVO> preview(@PathVariable Long postId) {
        return ApiResponse.ok(postService.preview(postId));
    }

    // 物理删除：只允许草稿与已撤回，且会同步解除媒体引用
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<Void> delete(@PathVariable Long postId) {
        postService.delete(postId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{postId}/publish")
    @PreAuthorize("hasAuthority('" + BlogPermissions.PUBLISH + "')")
    public ApiResponse<Void> publish(@PathVariable Long postId) {
        publishService.publish(postId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{postId}/withdraw")
    @PreAuthorize("hasAuthority('" + BlogPermissions.WITHDRAW + "')")
    public ApiResponse<Void> withdraw(@PathVariable Long postId) {
        publishService.withdraw(postId);
        return ApiResponse.ok(null);
    }

    // 恢复等同于重新发布，因此复用 blog:publish 权限
    @PostMapping("/{postId}/restore")
    @PreAuthorize("hasAuthority('" + BlogPermissions.PUBLISH + "')")
    public ApiResponse<Void> restore(@PathVariable Long postId) {
        publishService.restore(postId);
        return ApiResponse.ok(null);
    }
}

/*
 * BlogAdminPostController：博客模块的HTTP 接入文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：BlogPermissions、BlogPostBodyDTO、BlogPostCreateDTO、BlogPostQueryDTO、BlogPostUpdateDTO、BlogPostService、BlogPublishService。
 * 阅读入口：page（分页读取博客后台文章）；create（创建博客后台文章）；detail（读取详情博客后台文章）；update（更新博客后台文章）。
 */

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
/**
 * 博客后台文章控制器，属于博客模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin/blog/posts")
public class BlogAdminPostController {

    /**
     * 文章业务服务。
     * 协作类型：<code>BlogPostService</code>。
     */
    private final BlogPostService postService;
    /**
     * 发布业务服务。
     * 协作类型：<code>BlogPublishService</code>。
     */
    private final BlogPublishService publishService;

    /**
     * 构造BlogAdminPostController，接收本类使用的依赖或初始化参数。
     *
     * @param postService 文章业务服务；类型为 <code>BlogPostService</code>
     * @param publishService 发布业务服务；类型为 <code>BlogPublishService</code>
     */
    public BlogAdminPostController(BlogPostService postService, BlogPublishService publishService) {
        this.postService = postService;
        this.publishService = publishService;
    }

    // 后台列表：分页 + 关键词 + 状态 + 标签 + 专题
    /**
     * 分页读取博客后台文章。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>postService.page</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPostQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;BlogPostAdminVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogPostAdminVO>> page(BlogPostQueryDTO query) {
        return ApiResponse.ok(postService.page(query));
    }

    /**
     * 创建博客后台文章。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>postService.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostCreateDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostAdminVO&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminVO> create(@RequestBody BlogPostCreateDTO request) {
        return ApiResponse.ok(postService.create(request));
    }

    /**
     * 读取详情博客后台文章。
     * HTTP 映射：<code>@GetMapping("/{postId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>postService.detail</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostAdminDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<BlogPostAdminDetailVO> detail(@PathVariable Long postId) {
        return ApiResponse.ok(postService.detail(postId));
    }

    /**
     * 更新博客后台文章。
     * HTTP 映射：<code>@PatchMapping("/{postId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>postService.update</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostUpdateDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostAdminVO&gt;</code> 的处理结果
     */
    @PatchMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminVO> update(@PathVariable Long postId,
                                              @RequestBody BlogPostUpdateDTO request) {
        return ApiResponse.ok(postService.update(postId, request));
    }

    // 正文单独提交：避免每次小改标题都要重传整篇 Markdown
    /**
     * 更新正文。
     * HTTP 映射：<code>@PutMapping("/{postId}/body")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>postService.updateBody</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostBodyDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostAdminDetailVO&gt;</code> 的处理结果
     */
    @PutMapping("/{postId}/body")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<BlogPostAdminDetailVO> updateBody(@PathVariable Long postId,
                                                        @RequestBody BlogPostBodyDTO request) {
        return ApiResponse.ok(postService.updateBody(postId, request));
    }

    /**
     * 预览博客后台文章。
     * HTTP 映射：<code>@GetMapping("/{postId}/preview")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>postService.preview</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostAdminDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/{postId}/preview")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<BlogPostAdminDetailVO> preview(@PathVariable Long postId) {
        return ApiResponse.ok(postService.preview(postId));
    }

    // 物理删除：只允许草稿与已撤回，且会同步解除媒体引用
    /**
     * 删除博客后台文章。
     * HTTP 映射：<code>@DeleteMapping("/{postId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>postService.delete</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.EDIT + "')")
    public ApiResponse<Void> delete(@PathVariable Long postId) {
        postService.delete(postId);
        return ApiResponse.ok(null);
    }

    /**
     * 发布博客后台文章。
     * HTTP 映射：<code>@PostMapping("/{postId}/publish")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.PUBLISH + "\')")</code>。
     * 主要协作调用：<code>publishService.publish</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{postId}/publish")
    @PreAuthorize("hasAuthority('" + BlogPermissions.PUBLISH + "')")
    public ApiResponse<Void> publish(@PathVariable Long postId) {
        publishService.publish(postId);
        return ApiResponse.ok(null);
    }

    /**
     * 撤回博客后台文章。
     * HTTP 映射：<code>@PostMapping("/{postId}/withdraw")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.WITHDRAW + "\')")</code>。
     * 主要协作调用：<code>publishService.withdraw</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{postId}/withdraw")
    @PreAuthorize("hasAuthority('" + BlogPermissions.WITHDRAW + "')")
    public ApiResponse<Void> withdraw(@PathVariable Long postId) {
        publishService.withdraw(postId);
        return ApiResponse.ok(null);
    }

    // 恢复等同于重新发布，因此复用 blog:publish 权限
    /**
     * 恢复博客后台文章。
     * HTTP 映射：<code>@PostMapping("/{postId}/restore")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.PUBLISH + "\')")</code>。
     * 主要协作调用：<code>publishService.restore</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{postId}/restore")
    @PreAuthorize("hasAuthority('" + BlogPermissions.PUBLISH + "')")
    public ApiResponse<Void> restore(@PathVariable Long postId) {
        publishService.restore(postId);
        return ApiResponse.ok(null);
    }
}

/*
 * BlogAdminTopicController：博客模块的HTTP 接入文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：BlogPermissions、BlogTopicDTO、BlogTopicMemberRow、BlogTopicOrderDTO、BlogTopicQueryDTO、BlogTopicNavigationOrderDTO、BlogTopicService。
 * 阅读入口：page（分页读取博客后台专题）；create（创建博客后台专题）；update（更新博客后台专题）；reorderTopics（调整顺序专题列表）。
 */

package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.constant.BlogPermissions;
import com.starrainnotes.blog.dto.BlogTopicDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderDTO;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.dto.BlogTopicNavigationOrderDTO;
import com.starrainnotes.blog.service.BlogTopicService;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import java.util.List;
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
 * BLOG-005 / BLOG-006 / BLOG-007 专题接口。
 *
 * DELETE 只对**空专题**开放（成员数为 0），有成员返回 409 BLOG_TOPIC_NOT_EMPTY：
 * 专题承载成员与人工顺序，物理删除会连带毁掉策展结果，所以要求调用方先显式移出文章。
 * 只是想下架、还要保住成员与顺序时用 disable（恢复后照旧）。
 *
 * 排序端点放在 /posts/order，而不是给每个成员单独 PATCH 序号：
 * 前端拖拽产生的是一次完整的新顺序，逐个提交会产生中间态不一致。
 */
/**
 * 博客后台专题控制器，属于博客模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin/blog/topics")
public class BlogAdminTopicController {

    /**
     * 专题业务服务。
     * 协作类型：<code>BlogTopicService</code>。
     */
    private final BlogTopicService topicService;

    /**
     * 构造BlogAdminTopicController，接收本类使用的依赖或初始化参数。
     *
     * @param topicService 专题业务服务；类型为 <code>BlogTopicService</code>
     */
    public BlogAdminTopicController(BlogTopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * 分页读取博客后台专题。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>topicService.page</code>。
     *
     * @param query 查询参数；类型为 <code>BlogTopicQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;BlogTopicVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogTopicVO>> page(BlogTopicQueryDTO query) {
        return ApiResponse.ok(topicService.page(query));
    }

    /**
     * 创建博客后台专题。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogTopicVO&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTopicVO> create(@RequestBody BlogTopicDTO request) {
        return ApiResponse.ok(topicService.create(request));
    }

    /**
     * 更新博客后台专题。
     * HTTP 映射：<code>@PatchMapping("/{topicId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.update</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicDTO</code>
     * @return 类型为 <code>ApiResponse&lt;BlogTopicVO&gt;</code> 的处理结果
     */
    @PatchMapping("/{topicId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTopicVO> update(@PathVariable Long topicId, @RequestBody BlogTopicDTO request) {
        return ApiResponse.ok(topicService.update(topicId, request));
    }

    /**
     * 调整顺序专题列表。
     * HTTP 映射：<code>@PutMapping("/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.reorderTopics</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicNavigationOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PutMapping("/order")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> reorderTopics(@RequestBody BlogTopicNavigationOrderDTO request) {
        topicService.reorderTopics(request.getTopicIds());
        return ApiResponse.ok(null);
    }

    /*
     * 删除专题：仅空专题可删，有成员返回 409 BLOG_TOPIC_NOT_EMPTY。
     * 与 disable 的分工：删除是「这个专题不要了」，停用是「暂时不展示，成员与顺序留着」。
     */
    /**
     * 删除博客后台专题。
     * HTTP 映射：<code>@DeleteMapping("/{topicId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.delete</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> delete(@PathVariable Long topicId) {
        topicService.delete(topicId);
        return ApiResponse.ok(null);
    }

    /**
     * 禁用博客后台专题。
     * HTTP 映射：<code>@PostMapping("/{topicId}/disable")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.disable</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{topicId}/disable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> disable(@PathVariable Long topicId) {
        topicService.disable(topicId);
        return ApiResponse.ok(null);
    }

    /**
     * 启用博客后台专题。
     * HTTP 映射：<code>@PostMapping("/{topicId}/enable")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.enable</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{topicId}/enable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> enable(@PathVariable Long topicId) {
        topicService.enable(topicId);
        return ApiResponse.ok(null);
    }

    // 成员列表按人工顺序返回，排序界面据此渲染
    /**
     * members处理入口。
     * HTTP 映射：<code>@GetMapping("/{topicId}/posts")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>topicService.members</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;BlogTopicMemberRow&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/{topicId}/posts")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<List<BlogTopicMemberRow>> members(@PathVariable Long topicId) {
        return ApiResponse.ok(topicService.members(topicId));
    }

    /**
     * 添加文章。
     * HTTP 映射：<code>@PostMapping("/{topicId}/posts/{postId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.addPost</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{topicId}/posts/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> addPost(@PathVariable Long topicId, @PathVariable Long postId) {
        topicService.addPost(topicId, postId);
        return ApiResponse.ok(null);
    }

    /**
     * 移除文章。
     * HTTP 映射：<code>@DeleteMapping("/{topicId}/posts/{postId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.removePost</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{topicId}/posts/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> removePost(@PathVariable Long topicId, @PathVariable Long postId) {
        topicService.removePost(topicId, postId);
        return ApiResponse.ok(null);
    }

    /**
     * 调整顺序博客后台专题。
     * HTTP 映射：<code>@PutMapping("/{topicId}/posts/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + BlogPermissions.TAXONOMY_MANAGE + "\')")</code>。
     * 主要协作调用：<code>topicService.reorder</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PutMapping("/{topicId}/posts/order")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> reorder(@PathVariable Long topicId, @RequestBody BlogTopicOrderDTO request) {
        topicService.reorder(topicId, request.getPostIds());
        return ApiResponse.ok(null);
    }
}

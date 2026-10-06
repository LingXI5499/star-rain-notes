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
@RestController
@RequestMapping("/api/admin/blog/topics")
public class BlogAdminTopicController {

    private final BlogTopicService topicService;

    public BlogAdminTopicController(BlogTopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<BlogTopicVO>> page(BlogTopicQueryDTO query) {
        return ApiResponse.ok(topicService.page(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTopicVO> create(@RequestBody BlogTopicDTO request) {
        return ApiResponse.ok(topicService.create(request));
    }

    @PatchMapping("/{topicId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<BlogTopicVO> update(@PathVariable Long topicId, @RequestBody BlogTopicDTO request) {
        return ApiResponse.ok(topicService.update(topicId, request));
    }

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
    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> delete(@PathVariable Long topicId) {
        topicService.delete(topicId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{topicId}/disable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> disable(@PathVariable Long topicId) {
        topicService.disable(topicId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{topicId}/enable")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> enable(@PathVariable Long topicId) {
        topicService.enable(topicId);
        return ApiResponse.ok(null);
    }

    // 成员列表按人工顺序返回，排序界面据此渲染
    @GetMapping("/{topicId}/posts")
    @PreAuthorize("hasAuthority('" + BlogPermissions.READ_ADMIN + "')")
    public ApiResponse<List<BlogTopicMemberRow>> members(@PathVariable Long topicId) {
        return ApiResponse.ok(topicService.members(topicId));
    }

    @PostMapping("/{topicId}/posts/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> addPost(@PathVariable Long topicId, @PathVariable Long postId) {
        topicService.addPost(topicId, postId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{topicId}/posts/{postId}")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> removePost(@PathVariable Long topicId, @PathVariable Long postId) {
        topicService.removePost(topicId, postId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{topicId}/posts/order")
    @PreAuthorize("hasAuthority('" + BlogPermissions.TAXONOMY_MANAGE + "')")
    public ApiResponse<Void> reorder(@PathVariable Long topicId, @RequestBody BlogTopicOrderDTO request) {
        topicService.reorder(topicId, request.getPostIds());
        return ApiResponse.ok(null);
    }
}

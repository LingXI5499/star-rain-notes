package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogCommandService;
import com.starrainnotes.blog.service.BlogQueryService;
import com.starrainnotes.blog.service.BlogUpdateWorkflowService;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin blog post management; lifecycle remains limited to action endpoints. */
@RestController
@RequestMapping("/api/v1/admin/blog/posts")
@RequiredArgsConstructor
@Tag(name = "后台博客管理")
public class BlogAdminController {
    private final BlogCommandService commandService;
    private final BlogQueryService queryService;
    private final BlogUpdateWorkflowService updateWorkflowService;

    @GetMapping
    @Operation(summary = "查询博客文章")
    public BlogPostAdminPageVO list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) String status, @RequestParam(required = false) String tag,
                                    @RequestParam(required = false) String q) {
        return queryService.adminList(page, pageSize, status, tag, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建博客文章")
    public BlogPostAdminDetailVO create(@Valid @RequestBody CreatePostRequest request) {
        return commandService.create(request);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "查询博客文章详情")
    public BlogPostAdminDetailVO detail(@PathVariable Long postId) { return queryService.adminDetail(postId); }

    @PutMapping("/{postId}")
    @Operation(summary = "更新博客文章")
    public ResponseEntity<?> update(@PathVariable Long postId, @Valid @RequestBody UpdatePostRequest request,
                                    Authentication authentication) {
        BlogUpdateWorkflowService.BlogUpdateOutcome outcome = updateWorkflowService.update(authentication, postId, request);
        return ResponseEntity.status(outcome.status()).body(outcome.body());
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除博客文章")
    public void delete(@PathVariable Long postId) { commandService.delete(postId); }

    @PostMapping("/{postId}/publish")
    @Operation(summary = "发布博客文章")
    public BlogPostAdminDetailVO publish(@PathVariable Long postId) { return commandService.publish(postId); }

    @PostMapping("/{postId}/withdraw")
    @Operation(summary = "撤回博客文章")
    public BlogPostAdminDetailVO withdraw(@PathVariable Long postId) { return commandService.withdraw(postId); }
}

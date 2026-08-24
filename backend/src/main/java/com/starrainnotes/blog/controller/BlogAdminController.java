package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.AdminPostDetailView;
import com.starrainnotes.blog.dto.AdminPostPageView;
import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/**
 * Admin blog post management (04 §11). Lifecycle only via /publish and
 * /withdraw actions.
 */
@RestController
@RequestMapping("/api/v1/admin/blog/posts")
public class BlogAdminController {

    private final BlogService blogService;

    public BlogAdminController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public AdminPostPageView list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String tag,
                                  @RequestParam(required = false) String q) {
        return blogService.adminList(page, pageSize, status, tag, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPostDetailView create(@Valid @RequestBody CreatePostRequest request) {
        return blogService.create(request);
    }

    @GetMapping("/{postId}")
    public AdminPostDetailView detail(@PathVariable Long postId) {
        return blogService.adminDetail(postId);
    }

    @PutMapping("/{postId}")
    public AdminPostDetailView update(@PathVariable Long postId,
                                      @Valid @RequestBody UpdatePostRequest request) {
        return blogService.update(postId, request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId) {
        blogService.delete(postId);
    }

    @PostMapping("/{postId}/publish")
    public AdminPostDetailView publish(@PathVariable Long postId) {
        return blogService.publish(postId);
    }

    @PostMapping("/{postId}/withdraw")
    public AdminPostDetailView withdraw(@PathVariable Long postId) {
        return blogService.withdraw(postId);
    }
}

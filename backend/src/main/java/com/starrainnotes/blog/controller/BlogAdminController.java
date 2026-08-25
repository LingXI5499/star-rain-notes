package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.AdminPostDetailView;
import com.starrainnotes.blog.dto.AdminPostPageView;
import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogService;
import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import jakarta.validation.Valid;
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

/**
 * Admin blog post management (04 §11). Lifecycle only via /publish and
 * /withdraw actions.
 */
@RestController
@RequestMapping("/api/v1/admin/blog/posts")
public class BlogAdminController {

    private final BlogService blogService;
    private final ContentReviewService reviewService;

    public BlogAdminController(BlogService blogService, ContentReviewService reviewService) {
        this.blogService = blogService;
        this.reviewService = reviewService;
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
    public ResponseEntity<?> update(@PathVariable Long postId,
                                    @Valid @RequestBody UpdatePostRequest request,
                                    Authentication authentication) {
        if (!isSuperAdmin(authentication) && "PUBLISHED".equals(blogService.publishStatus(postId))) {
            ContentReviewView review = reviewService.submitBlogUpdate(actorId(authentication), postId, request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(review);
        }
        return ResponseEntity.ok(blogService.update(postId, request));
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

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private Long actorId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                ? principal.getId() : null;
    }
}

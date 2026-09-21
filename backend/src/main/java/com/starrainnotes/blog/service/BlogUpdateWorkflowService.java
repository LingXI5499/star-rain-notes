package com.starrainnotes.blog.service;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Applies the frozen administrator review policy outside the HTTP layer. */
@Service
@RequiredArgsConstructor
public class BlogUpdateWorkflowService {
    private final BlogQueryService queryService;
    private final BlogCommandService commandService;
    private final ContentReviewService reviewService;

    public BlogUpdateOutcome update(Authentication actor, Long postId, UpdatePostRequest request) {
        if (!isSuperAdmin(actor) && BlogCommandService.PUBLISHED.equals(queryService.publishStatus(postId))) {
            return new ReviewSubmitted(reviewService.submitBlogUpdate(actorId(actor), postId, request));
        }
        return new Updated(commandService.update(postId, request));
    }

    private boolean isSuperAdmin(Authentication actor) {
        return actor != null && actor.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private Long actorId(Authentication actor) {
        return actor != null && actor.getPrincipal() instanceof AccountPrincipal principal ? principal.getId() : null;
    }

    public sealed interface BlogUpdateOutcome permits Updated, ReviewSubmitted {
        HttpStatus status();
        Object body();
    }

    public record Updated(BlogPostAdminDetailVO body) implements BlogUpdateOutcome {
        @Override public HttpStatus status() { return HttpStatus.OK; }
    }

    public record ReviewSubmitted(ContentReviewView body) implements BlogUpdateOutcome {
        @Override public HttpStatus status() { return HttpStatus.ACCEPTED; }
    }
}

package com.starrainnotes.tutorial.service;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** The frozen published-chapter review rule, independent of the web layer. */
@Service
@RequiredArgsConstructor
public class TutorialChapterUpdateWorkflowService {
    private final TutorialNodeService nodeService;
    private final ContentReviewService reviewService;

    public Outcome update(Authentication actor, Long tutorialId, Long chapterId, UpdateChapterRequest request) {
        if (!isSuperAdmin(actor) && "PUBLISHED".equals(nodeService.chapterPublishStatus(tutorialId, chapterId))) {
            return new ReviewSubmitted(reviewService.submitTutorialChapterUpdate(actorId(actor), tutorialId, chapterId, request));
        }
        return new Updated(nodeService.updateChapter(tutorialId, chapterId, request));
    }

    private boolean isSuperAdmin(Authentication actor) {
        return actor != null && actor.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private Long actorId(Authentication actor) {
        return actor != null && actor.getPrincipal() instanceof AccountPrincipal principal ? principal.getId() : null;
    }

    public sealed interface Outcome permits Updated, ReviewSubmitted { HttpStatus status(); Object body(); }
    public record Updated(ChapterDetailView body) implements Outcome { @Override public HttpStatus status() { return HttpStatus.OK; } }
    public record ReviewSubmitted(ContentReviewView body) implements Outcome { @Override public HttpStatus status() { return HttpStatus.ACCEPTED; } }
}

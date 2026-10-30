package com.starrainnotes.english.writing.article.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import com.starrainnotes.english.writing.article.mapper.WritingArticleMapper;
import com.starrainnotes.review.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

/** Uses an immutable document revision and the existing review permission/audit boundary. */
@Component
@RequiredArgsConstructor
public class EssayReviewHandler implements ReviewTargetHandler {
    private final WritingArticleMapper mapper;
    private final DocumentMetadataService metadata;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    @Override public String targetModule() { return "ENGLISH"; }
    @Override public String targetType() { return "ESSAY"; }
    @Override public boolean supports(String module, String type, String reviewType) {
        return targetModule().equals(module) && targetType().equals(type) && "english.essay.publish".equals(reviewType);
    }

    public record View(String viewType, String title, String summary, Article contentSnapshot, String executionStatus) implements ReviewTargetView { }

    @Override @Transactional
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        Article frozen = frozen(target);
        return new View("ENGLISH_ESSAY", frozen.getTitle(), frozen.getSummary(), frozen, pendingExecution(target, frozen));
    }

    private String pendingExecution(ReviewTargetRef target, Article frozen) {
        Article current=mapper.lockAny(target.getTargetId());
        return current!=null && "PENDING".equals(current.getState()) && current.getRowVersion()==frozen.getRowVersion()+1 ? "PENDING" : "APPLIED";
    }

    private Article frozen(ReviewTargetRef target) {
        var revision = metadata.revision("WRITING", target.getTargetId(), Long.parseLong(target.getRevisionRef()));
        return json.convertValue(revision.getSnapshot(), Article.class);
    }

    @Override @Transactional(propagation=Propagation.REQUIRES_NEW, isolation=Isolation.READ_COMMITTED) public void onApproved(ReviewDecisionContext context) { apply(context, "COMPLETED", "PUBLIC"); }
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW, isolation=Isolation.READ_COMMITTED) public void onRejected(ReviewDecisionContext context) { apply(context, "REJECTED", "PRIVATE"); }
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW, isolation=Isolation.READ_COMMITTED) public void onCanceled(ReviewDecisionContext context) { apply(context, "DRAFT", "PRIVATE"); }

    private void apply(ReviewDecisionContext context, String state, String visibility) {
        Article frozen = frozen(context.getTarget());
        Article current = mapper.lockAny(context.getTarget().getTargetId());
        // A cancellation can already have been applied synchronously by its applicant.
        if(current == null || !"PENDING".equals(current.getState())) return;
        if(!current.getOwnerAccountId().equals(context.getApplicantAccountId())
                || current.getRowVersion() != frozen.getRowVersion() + 1) {
            throw new ApiException("WRITING_VERSION_CONFLICT", "送审版本与当前文章不一致", 409);
        }
        if(mapper.status(current.getId(), current.getOwnerAccountId(), current.getRowVersion(), state, visibility) != 1) {
            throw new ApiException("WRITING_VERSION_CONFLICT", "文章已变化，请刷新后核对", 409);
        }
        events.publishEvent(new EnglishSearchContentChangedEvent("ENGLISH_WRITING", current.getId()));
    }

    @Override public boolean supportsApprovedRecovery() { return true; }
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW, isolation=Isolation.READ_COMMITTED) public void recoverApproved(ReviewDecisionContext context) { apply(context, "COMPLETED", "PUBLIC"); }
}

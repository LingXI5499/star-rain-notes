package com.starrainnotes.tutorial.content.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.content.vo.TutorialReviewVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TutorialReviewTargetHandler implements ReviewTargetHandler {
    private final TutorialMapper tutorialMapper;
    private final TutorialRevisionMapper revisionMapper;
    private final ObjectMapper objectMapper;
    private final TutorialEventPublisher eventPublisher;

    @Override
    public String targetModule() { return "TUTORIAL"; }

    @Override
    public String targetType() { return "TUTORIAL"; }

    @Override
    public boolean supports(String module, String type, String reviewType) {
        return "TUTORIAL".equals(module) && "TUTORIAL".equals(type)
                && "tutorial.publish".equals(reviewType);
    }

    private TutorialRevisionEntity revision(ReviewTargetRef target) {
        if (target == null || !supports(target.getTargetModule(), target.getTargetType(), "tutorial.publish")) {
            return null;
        }
        return revisionMapper.selectByTutorialIdAndRef(target.getTargetId(), target.getRevisionRef());
    }

    private boolean isLatestRevision(TutorialRevisionEntity revision) {
        TutorialRevisionEntity latest = revisionMapper.selectLatestByTutorialId(revision.getTutorialId());
        return latest != null && latest.getId().equals(revision.getId());
    }

    @Override
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        TutorialRevisionEntity row = revision(target);
        if (row == null) return null;
        try {
            JsonNode snapshot = objectMapper.readTree(row.getSnapshotJson());
            return new TutorialReviewVO(row.getRevisionRef(), snapshot.path("title").asText(), snapshot);
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程审核版本不可读取");
        }
    }

    @Override
    @Transactional
    public void onApproved(ReviewDecisionContext context) {
        TutorialRevisionEntity revision = revision(context.getTarget());
        if (revision == null) throw new TutorialStateException("审核版本不存在");
        TutorialEntity tutorial = tutorialMapper.byIdForUpdate(revision.getTutorialId());
        if (tutorial == null) throw new TutorialStateException("教程不存在");
        if (revision.getId().equals(tutorial.getPublishedRevisionId())
                && "PUBLISHED".equals(tutorial.getPublicationStatus())) return;
        if (!"IN_REVIEW".equals(tutorial.getEditingStatus()) || !isLatestRevision(revision)) {
            throw new TutorialStateException("教程不在审核中");
        }
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setEditingStatus("DRAFT");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.update(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorial.getId(), tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
    }

    @Override
    @Transactional
    public void onRejected(ReviewDecisionContext context) {
        resetReview(context);
    }

    @Override
    @Transactional
    public void onCanceled(ReviewDecisionContext context) {
        resetReview(context);
    }

    private void resetReview(ReviewDecisionContext context) {
        TutorialRevisionEntity revision = revision(context.getTarget());
        if (revision == null) return;
        TutorialEntity tutorial = tutorialMapper.byIdForUpdate(revision.getTutorialId());
        if (tutorial == null || !"IN_REVIEW".equals(tutorial.getEditingStatus())
                || !isLatestRevision(revision)) return;
        tutorial.setEditingStatus("DRAFT");
        tutorialMapper.update(tutorial);
    }
}

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.learning.mapper.LearningHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningEventWriter {
    private final LearningHistoryMapper historyMapper;

    public void session(Long accountId, com.starrainnotes.tutorial.learning.entity.EvidenceModels.Session session,
                        com.starrainnotes.tutorial.learning.entity.EvidenceModels.SessionSummary summary) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType("INITIAL_STUDY".equals(session.getSessionType()) ? "INITIAL_SESSION_COMPLETED" : "REVIEW_SESSION_COMPLETED");
        row.setTutorialId(session.getTutorialId());row.setChapterId(session.getChapterId());row.setStudyTaskId(session.getStudyTaskId());
        try { row.setDetailJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summary)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new IllegalStateException("学习摘要序列化失败", exception); }
        historyMapper.insert(row);
    }

    public void chapter(Long accountId, String eventType, LearningContentAccess.ChapterRef chapter) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType(eventType);
        row.setTutorialId(chapter.getTutorialId());
        row.setGroupId(chapter.getGroupId());
        row.setChapterId(chapter.getChapterId());
        historyMapper.insert(row);
    }

    public void task(Long accountId, String eventType, Long tutorialId, Long chapterId, Long taskId) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType(eventType);
        row.setTutorialId(tutorialId);
        row.setChapterId(chapterId);
        row.setStudyTaskId(taskId);
        historyMapper.insert(row);
    }

    public void review(Long accountId, Long tutorialId, Long chapterId, Long cardId, Long reviewTaskId,
                       String detailJson) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType("REVIEW_COMPLETED");
        row.setTutorialId(tutorialId);
        row.setChapterId(chapterId);
        row.setKnowledgeCardId(cardId);
        row.setReviewTaskId(reviewTaskId);
        row.setDetailJson(detailJson);
        historyMapper.insert(row);
    }
}

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.learning.mapper.LearningHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningEventWriter {
    private final LearningHistoryMapper historyMapper;

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

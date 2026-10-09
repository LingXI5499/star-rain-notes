package com.starrainnotes.tutorial.learning.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryVO {
    private String id;
    private String eventType;
    private String tutorialId;
    private String chapterId;
    private String cardId;
    private String studyTaskId;
    private String reviewTaskId;
    private LocalDateTime occurredAt;
}

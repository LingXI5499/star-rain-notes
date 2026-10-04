package com.starrainnotes.tutorial.learning.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressVO {
    private String tutorialId;
    private String chapterId;
    private String tutorialSlug;
    private String chapterSlug;
    private String chapterTitle;
    private String scrollAnchor;
    private BigDecimal progressRatio;
    private Long studySecondsTotal;
    private LocalDateTime completedAt;
    private LocalDateTime lastStudiedAt;
}

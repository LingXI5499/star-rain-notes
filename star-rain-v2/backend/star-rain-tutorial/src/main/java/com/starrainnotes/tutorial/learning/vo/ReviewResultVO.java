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
public class ReviewResultVO {
    private String taskId;
    private String cardId;
    private String rating;
    private int previousIntervalDays;
    private int nextIntervalDays;
    private LocalDateTime nextReviewAt;
    private String systemSuggestedLevel;
}

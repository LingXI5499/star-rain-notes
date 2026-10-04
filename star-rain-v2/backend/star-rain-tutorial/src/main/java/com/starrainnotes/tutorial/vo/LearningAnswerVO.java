package com.starrainnotes.tutorial.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAnswerVO {
    private String questionId;
    private String answerText;
    private LocalDateTime firstSubmittedAt;
    private LocalDateTime referenceUnlockedAt;
}

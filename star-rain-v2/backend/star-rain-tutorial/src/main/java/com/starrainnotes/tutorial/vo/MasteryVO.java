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
public class MasteryVO {
    private String cardId;
    private String chapterId;
    private String frontText;
    private String systemSuggestedLevel;
    private String userSelfLevel;
    private String lastRecallRating;
    private Integer evidenceCount;
    private LocalDateTime firstLearnedAt;
    private LocalDateTime lastEvidenceAt;
}

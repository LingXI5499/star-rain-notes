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
public class ReviewTaskVO {
    private String id;
    private String cardId;
    private String chapterId;
    private String tutorialSlug;
    private String chapterSlug;
    private String frontText;
    private LocalDateTime dueAt;
    private String status;
}

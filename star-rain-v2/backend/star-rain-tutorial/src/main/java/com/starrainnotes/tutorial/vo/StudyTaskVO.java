package com.starrainnotes.tutorial.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyTaskVO {
    private String id;
    private String planId;
    private String tutorialId;
    private String chapterId;
    private String tutorialSlug;
    private String chapterSlug;
    private String chapterTitle;
    private LocalDate taskDate;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}

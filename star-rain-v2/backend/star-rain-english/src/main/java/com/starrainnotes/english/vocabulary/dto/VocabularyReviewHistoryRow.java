package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一条真实复习记录（仓储 -> 服务）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewHistoryRow {

    private Long id;

    private Long wordId;

    private String word;

    private int reviewNumber;

    private String direction;

    private LocalDateTime scheduledAt;

    private LocalDateTime reviewedAt;

    private long intervalSeconds;

    private String timingStatus;
}

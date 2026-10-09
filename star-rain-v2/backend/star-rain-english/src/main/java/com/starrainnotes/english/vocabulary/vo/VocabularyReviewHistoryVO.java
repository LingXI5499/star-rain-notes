package com.starrainnotes.english.vocabulary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一条真实复习记录（进度页「最近真实复习」）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewHistoryVO {
    private String rating;
    private String source;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    private String word;

    private int reviewNumber;

    private String direction;

    private String scheduledAt;

    private String reviewedAt;

    private long intervalSeconds;

    private String timingStatus;
}

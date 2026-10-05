package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次复习的排期结果（层间传输对象：策略 -> 仓储）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewSchedule {

    /* 第几次复习，从 1 开始 */
    private int reviewNumber;

    /* 固定间隔档位，上限 10 */
    private int reviewStep;

    private long intervalSeconds;

    /* 计划复习时间；首次复习为 null */
    private LocalDateTime scheduledAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime nextReviewAt;

    /* NEW / EARLY / ON_TIME / OVERDUE */
    private String timingStatus;
}

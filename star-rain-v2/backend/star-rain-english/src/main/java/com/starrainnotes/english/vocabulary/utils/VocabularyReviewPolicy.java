package com.starrainnotes.english.vocabulary.utils;

import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.enumeration.TimingStatus;
import java.time.LocalDateTime;

/*
 * 固定间隔复习策略。逐值对齐 V1 VocabularyReviewPolicy：
 * 复习序号递增 -> 取固定间隔表的对应档位 -> 下次复习时间 = 本次复习时间 + 间隔。
 * 时机判定同样照抄 V1 口径，历史 interval_seconds / timing_status 才能连续。
 */
public final class VocabularyReviewPolicy {

    private VocabularyReviewPolicy() { }

    public static VocabularyReviewSchedule next(int currentReviewCount, LocalDateTime scheduledAt,
                                               LocalDateTime reviewedAt) {
        int reviewNumber = currentReviewCount + 1;
        long interval = intervalSeconds(reviewNumber);
        String timing = timingStatus(scheduledAt, reviewedAt);
        return VocabularyReviewSchedule.builder()
                .reviewNumber(reviewNumber)
                .reviewStep(Math.min(reviewNumber, VocabularyStudyConstants.MAX_REVIEW_STEP))
                .intervalSeconds(interval)
                .scheduledAt(scheduledAt)
                .reviewedAt(reviewedAt)
                .nextReviewAt(reviewedAt.plusSeconds(interval))
                .timingStatus(timing)
                .build();
    }

    /*
     * 第 reviewNumber 次复习使用的间隔秒数；序号越界时钳到 1..10。
     */
    public static long intervalSeconds(int reviewNumber) {
        int index = Math.max(1, Math.min(reviewNumber, VocabularyStudyConstants.MAX_REVIEW_STEP)) - 1;
        return VocabularyStudyConstants.REVIEW_INTERVAL_SECONDS[index];
    }

    /*
     * 时机判定：无计划时间为 NEW；早于计划为 EARLY；超过计划 24 小时为 OVERDUE；其余 ON_TIME。
     */
    public static String timingStatus(LocalDateTime scheduledAt, LocalDateTime reviewedAt) {
        if (scheduledAt == null) {
            return TimingStatus.NEW.name();
        }
        if (reviewedAt.isBefore(scheduledAt)) {
            return TimingStatus.EARLY.name();
        }
        if (reviewedAt.isAfter(scheduledAt.plusSeconds(VocabularyStudyConstants.OVERDUE_GRACE_SECONDS))) {
            return TimingStatus.OVERDUE.name();
        }
        return TimingStatus.ON_TIME.name();
    }
}

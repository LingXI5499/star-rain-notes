package com.starrainnotes.english.vocabulary.learning.domain;

import java.time.LocalDateTime;

/** Preserves the existing ten-stage review schedule and timing categories. */
public final class VocabularyReviewPolicy {
    private static final long[] REVIEW_INTERVAL_SECONDS = {
            300L, 1_800L, 43_200L, 86_400L, 172_800L,
            345_600L, 604_800L, 1_296_000L, 2_592_000L, 5_184_000L
    };

    public ReviewSchedule next(int currentReviewCount, LocalDateTime scheduledAt, LocalDateTime reviewedAt) {
        int reviewNumber = currentReviewCount + 1;
        long interval = intervalSeconds(reviewNumber);
        String timing = scheduledAt == null ? "NEW" : reviewedAt.isBefore(scheduledAt) ? "EARLY"
                : reviewedAt.isAfter(scheduledAt.plusHours(24)) ? "OVERDUE" : "ON_TIME";
        return new ReviewSchedule(reviewNumber, Math.min(reviewNumber, 10), interval,
                scheduledAt, reviewedAt, reviewedAt.plusSeconds(interval), timing);
    }

    public static long intervalSeconds(int reviewNumber) {
        int index = Math.max(1, Math.min(reviewNumber, REVIEW_INTERVAL_SECONDS.length)) - 1;
        return REVIEW_INTERVAL_SECONDS[index];
    }
}

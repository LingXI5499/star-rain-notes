package com.starrainnotes.english.vocabulary.learning;

import java.time.Duration;
import java.time.LocalDateTime;

/** Configurable-in-code product intervals; no scheduler and no automatic overdue progression. */
public final class VocabularyHighIntensityReviewPolicy {
    public static final int MASTERED_STEP = 14;
    private static final long[] INTERVAL_SECONDS = {
        300, 1200, 3600, 14400, 43200, 86400, 172800, 259200,
        432000, 604800, 864000, 1209600, 1814400, 2419200
    };
    public static final long MASTERED_INTERVAL_SECONDS = 35L * 86400;
    private VocabularyHighIntensityReviewPolicy() { }

    public static int nextStep(int previous, String rating, boolean graduated) {
        int current = Math.max(-1, Math.min(MASTERED_STEP, previous));
        return switch (rating) {
            case "FORGOT" -> 0;
            case "UNCERTAIN" -> current == MASTERED_STEP ? 2 : Math.max(0, current - 2);
            case "KNOW" -> graduated ? MASTERED_STEP : Math.min(13, current + 1);
            default -> throw new IllegalArgumentException("Unknown rating");
        };
    }

    public static long intervalSeconds(int step) {
        if (step == MASTERED_STEP) { return MASTERED_INTERVAL_SECONDS; }
        if (step < 0 || step >= INTERVAL_SECONDS.length) { throw new IllegalArgumentException("Unknown step"); }
        return INTERVAL_SECONDS[step];
    }

    public static String timing(LocalDateTime due, LocalDateTime now) {
        if (due == null) { return "NEW"; }
        long seconds = Duration.between(due, now).getSeconds();
        return seconds < 0 ? "EARLY" : seconds <= 300 ? "ON_TIME" : "OVERDUE";
    }
}

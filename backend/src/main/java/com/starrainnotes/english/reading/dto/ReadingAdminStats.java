package com.starrainnotes.english.reading.dto;

import java.util.Map;

/**
 * Admin reading workbench summary, computed with aggregate queries (no N+1).
 */
public record ReadingAdminStats(
        long total,
        long published,
        long draft,
        long withdrawn,
        long missingExercise,
        Map<String, Long> byCefr) {
}

package com.starrainnotes.english.reading.dto;

import java.util.Map;

/**
 * Admin reading-exercise view. Includes the full config (with the correct
 * answer) because it is only served on authenticated admin endpoints.
 */
public record ReadingExerciseView(
        Long id,
        Long articleId,
        String questionType,
        String promptMarkdown,
        Map<String, Object> config,
        String explanationMarkdown,
        Integer scoreValue,
        Integer sortOrder,
        String publishStatus,
        String updatedAt) {
}

package com.starrainnotes.english.shared.exercise.dto;

import java.util.Map;

/**
 * Admin exercise view. Includes the full config because it is only served on
 * authenticated admin endpoints.
 */
public record ExerciseView(
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

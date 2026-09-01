package com.starrainnotes.english.reading.dto;

import java.util.Map;

/**
 * Public reading-exercise view. Config is sanitized: any field that could
 * reveal the correct answer (answer / answers / correctIndexes / standard
 * ordering / fill answers) is stripped before it reaches an unauthenticated
 * client. Scoring happens server-side against the stored answer.
 */
public record ReadingExercisePublicView(
        Long id,
        String questionType,
        String promptMarkdown,
        Map<String, Object> config,
        Integer scoreValue,
        Integer sortOrder) {
}

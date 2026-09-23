package com.starrainnotes.english.shared.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for an exercise bound to one content item.
 * {@code configJson} is validated by the shared exercise service per question
 * type before persistence.
 */
public record ExerciseRequest(
        @NotBlank @Size(max = 50) String questionType,
        @NotBlank String promptMarkdown,
        @NotBlank String configJson,
        @Size(max = 5000) String explanationMarkdown,
        @NotNull Integer scoreValue,
        String publishStatus) {
}

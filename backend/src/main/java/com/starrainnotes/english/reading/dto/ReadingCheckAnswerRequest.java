package com.starrainnotes.english.reading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Public answer submission for one article's exercises. Only published
 * exercises are scored; the result is returned with per-item explanations.
 */
public record ReadingCheckAnswerRequest(
        @NotEmpty List<@Valid Submission> answers) {

    public record Submission(@NotNull Long exerciseId, Object answer) {
    }
}

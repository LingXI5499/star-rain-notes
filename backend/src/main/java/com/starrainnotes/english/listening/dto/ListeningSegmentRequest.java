package com.starrainnotes.english.listening.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ListeningSegmentRequest(
        @NotNull @Min(0) Integer startMs,
        @NotNull @Min(1) Integer endMs,
        @NotBlank String transcriptText,
        String translationText) {
}

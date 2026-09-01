package com.starrainnotes.english.listening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReadingPairRequest(
        @NotNull Long readingArticleId,
        @NotBlank String relationType) {
}

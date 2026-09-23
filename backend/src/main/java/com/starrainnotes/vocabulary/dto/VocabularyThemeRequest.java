package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabularyThemeRequest(
        @Min(1) @Max(6) int layerOrder,
        @NotBlank @Size(max = 200) String name,
        @Min(0) Integer sortOrder) {
}

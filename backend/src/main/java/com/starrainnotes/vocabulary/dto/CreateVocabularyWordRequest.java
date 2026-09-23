package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateVocabularyWordRequest(
        @NotNull @Positive Long themeId,
        @Size(max = 50) String partOfSpeech,
        @NotBlank @Size(max = 200) String word,
        @Size(max = 100) String phoneticUs,
        @Size(max = 100) String phoneticUk,
        @NotBlank @Size(max = 1000) String translation,
        @Size(max = 1000) String sceneMeaning,
        @Size(max = 1000) String inflections) {
}

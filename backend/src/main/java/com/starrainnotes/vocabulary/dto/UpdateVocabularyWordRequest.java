package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin edit of all editable vocabulary placement fields.
 */
public record UpdateVocabularyWordRequest(
        Long themeId,
        @Size(max = 50) String partOfSpeech,
        @Size(max = 200) String word,
        @NotBlank @Size(max = 1000) String translation,
        @Size(max = 1000) String sceneMeaning,
        @Size(max = 100) String phoneticUs,
        @Size(max = 100) String phoneticUk,
        @Size(max = 1000) String inflections) {
}

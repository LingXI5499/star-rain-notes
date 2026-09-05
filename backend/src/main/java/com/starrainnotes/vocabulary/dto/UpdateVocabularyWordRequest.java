package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin edit of a word's readable fields (core word text is treated as
 * seed data and not editable in the MVP).
 */
public record UpdateVocabularyWordRequest(
        @NotBlank @Size(max = 1000) String translation,
        @Size(max = 100) String phoneticUs,
        @Size(max = 100) String phoneticUk,
        @Size(max = 1000) String inflections) {
}

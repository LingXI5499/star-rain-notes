package com.starrainnotes.english.vocabulary.dto;

/**
 * One theme card in the public category listing.
 */
public record VocabularyThemeView(
        Long id,
        String name,
        long wordCount) {
}

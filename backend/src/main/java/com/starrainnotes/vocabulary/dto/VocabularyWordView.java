package com.starrainnotes.vocabulary.dto;

import com.starrainnotes.vocabulary.entity.VocabularyExample;

import java.util.List;

/**
 * One vocabulary card (public + admin share this shape; memory fields are
 * public so the owner can review and tap +1 on the card).
 */
public record VocabularyWordView(
        Long id,
        Long themeId,
        String partOfSpeech,
        String word,
        String phoneticUs,
        String translation,
        String inflections,
        List<VocabularyExample> examples,
        int memoryCount,
        String lastMemoryAt) {
}

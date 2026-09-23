package com.starrainnotes.english.vocabulary.dto;

import com.starrainnotes.english.vocabulary.entity.VocabularyExample;

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
        String phoneticUk,
        String translation,
        String sceneMeaning,
        String inflections,
        List<VocabularyExample> examples,
        int memoryCount,
        String lastMemoryAt,
        List<VocabularyAudioView> audios,
        List<VocabularyFamilyView> wordFamilies) {
}

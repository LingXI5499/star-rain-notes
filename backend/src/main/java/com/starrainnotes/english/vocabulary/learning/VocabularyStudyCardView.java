package com.starrainnotes.english.vocabulary.learning;

import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;

public record VocabularyStudyCardView(
        VocabularyWordView word,
        VocabularyMemoryView memory,
        String direction,
        boolean newWord) {
}

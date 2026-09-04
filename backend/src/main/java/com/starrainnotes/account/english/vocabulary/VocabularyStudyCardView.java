package com.starrainnotes.account.english.vocabulary;

import com.starrainnotes.vocabulary.dto.VocabularyWordView;

public record VocabularyStudyCardView(
        VocabularyWordView word,
        VocabularyMemoryView memory,
        String direction,
        boolean newWord) {
}

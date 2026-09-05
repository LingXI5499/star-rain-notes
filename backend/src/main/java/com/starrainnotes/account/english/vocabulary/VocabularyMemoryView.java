package com.starrainnotes.account.english.vocabulary;

public record VocabularyMemoryView(
        Long wordId,
        int memoryCount,
        int reviewStep,
        int reviewCount,
        String firstLearnedAt,
        String lastReviewedAt,
        String nextReviewAt,
        String learningStatus,
        String displayMode) {
}

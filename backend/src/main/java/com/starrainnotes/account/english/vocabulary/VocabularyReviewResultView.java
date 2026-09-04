package com.starrainnotes.account.english.vocabulary;

public record VocabularyReviewResultView(
        VocabularyMemoryView memory,
        int reviewNumber,
        long intervalSeconds,
        String timingStatus,
        boolean duplicate) {
}

package com.starrainnotes.english.learning.vocabulary;

public record VocabularyReviewResultView(
        VocabularyMemoryView memory,
        int reviewNumber,
        long intervalSeconds,
        String timingStatus,
        boolean duplicate) {
}

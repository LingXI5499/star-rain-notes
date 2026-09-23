package com.starrainnotes.english.vocabulary.learning;

public record VocabularyReviewResultView(
        VocabularyMemoryView memory,
        int reviewNumber,
        long intervalSeconds,
        String timingStatus,
        boolean duplicate) {
}

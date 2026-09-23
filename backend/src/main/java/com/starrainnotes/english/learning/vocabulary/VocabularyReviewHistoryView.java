package com.starrainnotes.english.learning.vocabulary;

public record VocabularyReviewHistoryView(
        Long id,
        Long wordId,
        String word,
        int reviewNumber,
        String direction,
        String scheduledAt,
        String reviewedAt,
        long intervalSeconds,
        String timingStatus) {
}

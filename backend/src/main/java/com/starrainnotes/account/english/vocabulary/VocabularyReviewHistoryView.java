package com.starrainnotes.account.english.vocabulary;

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

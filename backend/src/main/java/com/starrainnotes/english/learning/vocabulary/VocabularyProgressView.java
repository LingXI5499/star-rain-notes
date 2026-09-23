package com.starrainnotes.english.learning.vocabulary;

import java.util.List;

public record VocabularyProgressView(
        long activeWords,
        long dueWords,
        long completedToday,
        long totalReviews,
        long totalMemoryCount,
        List<VocabularyReviewHistoryView> recentReviews) {
}

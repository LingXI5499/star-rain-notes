package com.starrainnotes.account.english.vocabulary;

import java.util.List;

public record VocabularyProgressView(
        long activeWords,
        long dueWords,
        long completedToday,
        long totalReviews,
        long totalMemoryCount,
        List<VocabularyReviewHistoryView> recentReviews) {
}

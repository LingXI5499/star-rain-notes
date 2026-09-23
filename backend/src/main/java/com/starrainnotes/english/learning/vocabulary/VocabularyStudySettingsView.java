package com.starrainnotes.english.learning.vocabulary;

public record VocabularyStudySettingsView(
        boolean showEnglish,
        boolean showChinese,
        String reviewDirection,
        int dailyNewLimit,
        int dailyReviewLimit) {
}

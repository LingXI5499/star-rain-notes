package com.starrainnotes.english.vocabulary.learning;

public record VocabularyStudySettingsView(
        boolean showEnglish,
        boolean showChinese,
        String reviewDirection,
        int dailyNewLimit,
        int dailyReviewLimit) {
}

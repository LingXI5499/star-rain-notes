package com.starrainnotes.account.english.vocabulary;

public record VocabularyStudySettingsView(
        boolean showEnglish,
        boolean showChinese,
        String reviewDirection,
        int dailyNewLimit,
        int dailyReviewLimit) {
}

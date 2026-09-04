package com.starrainnotes.account.english.vocabulary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VocabularyStudySettingsRequest(
        boolean showEnglish,
        boolean showChinese,
        @NotBlank @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|MIXED") String reviewDirection,
        @Min(0) @Max(200) int dailyNewLimit,
        @Min(1) @Max(1000) int dailyReviewLimit) {
}

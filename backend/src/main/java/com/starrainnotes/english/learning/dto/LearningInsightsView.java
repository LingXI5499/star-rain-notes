package com.starrainnotes.english.learning.dto;

import java.math.BigDecimal;
import java.util.List;

public record LearningInsightsView(
        long totalTimeSeconds, long totalAttempts, int activeDays14, int currentStreak,
        BigDecimal averageScore, BigDecimal averageMastery,
        List<LearningActivityDayView> activity,
        List<LearningModuleInsightView> modules,
        List<LearningRecommendationView> recommendations) {
}

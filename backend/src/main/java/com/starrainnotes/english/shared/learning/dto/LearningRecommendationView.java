package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;

public record LearningRecommendationView(
        String contentType, Long contentId, String slug, String title,
        String route, String reason, String cefrLevel,
        BigDecimal mastery, String nextReviewAt,
        String recommendationType, int priority, String sourceTitle) {
}

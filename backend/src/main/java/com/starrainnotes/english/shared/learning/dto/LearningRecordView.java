package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;
import java.util.List;

public record LearningRecordView(
        Long id, String contentType, Long contentId, String contentSlug, String cefrLevel,
        String status, BigDecimal score, int timeSpentSeconds, int attemptCount,
        List<String> weakPoints, BigDecimal mastery, String nextReviewAt, String updatedAt) {
}

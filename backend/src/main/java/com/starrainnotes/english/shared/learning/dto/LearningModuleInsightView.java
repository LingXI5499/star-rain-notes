package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;

public record LearningModuleInsightView(
        String contentType, long total, long completed,
        BigDecimal averageMastery, long timeSpentSeconds) {
}

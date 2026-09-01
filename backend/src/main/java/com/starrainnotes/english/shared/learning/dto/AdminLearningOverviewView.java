package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;

public record AdminLearningOverviewView(
        long activeLearners,
        long totalAttempts,
        long completions,
        BigDecimal completionRate,
        long totalTimeSeconds
) { }

package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;

public record AdminLearningModuleView(
        String contentType,
        long publishedContent,
        long engagedContent,
        long attempts,
        long completions,
        BigDecimal completionRate,
        long timeSpentSeconds
) { }

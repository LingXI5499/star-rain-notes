package com.starrainnotes.english.shared.learning.dto;

import java.util.List;

public record AdminLearningAnalyticsView(
        int days,
        String contentType,
        String generatedAt,
        AdminLearningOverviewView overview,
        List<AdminLearningTrendDayView> trend,
        List<AdminLearningModuleView> modules
) { }

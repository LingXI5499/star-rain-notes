package com.starrainnotes.english.shared.learning.dto;

public record AdminLearningTrendDayView(
        String date,
        long attempts,
        long completions,
        long activeLearners,
        long timeSpentSeconds
) { }

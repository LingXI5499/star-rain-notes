package com.starrainnotes.english.learning.dto;

public record AdminLearningTrendDayView(
        String date,
        long attempts,
        long completions,
        long activeLearners,
        long timeSpentSeconds
) { }

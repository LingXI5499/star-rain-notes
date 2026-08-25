package com.starrainnotes.english.shared.learning.dto;

public record LearningActivityDayView(
        String date, int attempts, int completed, int timeSpentSeconds) {
}

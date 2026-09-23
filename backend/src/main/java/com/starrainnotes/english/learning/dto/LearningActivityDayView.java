package com.starrainnotes.english.learning.dto;

public record LearningActivityDayView(
        String date, int attempts, int completed, int timeSpentSeconds) {
}

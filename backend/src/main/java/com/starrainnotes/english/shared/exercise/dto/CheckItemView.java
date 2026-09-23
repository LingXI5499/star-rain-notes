package com.starrainnotes.english.shared.exercise.dto;

public record CheckItemView(
        Long exerciseId,
        boolean correct,
        int earned,
        int scoreValue,
        String explanationMarkdown) {
}

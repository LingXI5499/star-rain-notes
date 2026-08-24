package com.starrainnotes.english.reading.dto;

public record ReadingCheckItemView(
        Long exerciseId,
        boolean correct,
        int earned,
        int scoreValue,
        String explanationMarkdown) {
}

package com.starrainnotes.english.shared.cefr.dto;

public record CefrLevelView(
        String level,
        Integer vocabMin,
        Integer vocabMax,
        Integer readingSentenceMin,
        Integer readingSentenceMax,
        Integer listeningWpmMin,
        Integer listeningWpmMax,
        Integer writingLengthMin,
        Integer writingLengthMax,
        String description) {
}

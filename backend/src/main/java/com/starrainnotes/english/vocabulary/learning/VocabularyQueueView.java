package com.starrainnotes.english.vocabulary.learning;

import java.util.List;

public record VocabularyQueueView(
        List<VocabularyStudyCardView> items,
        int dueCount,
        int newCount,
        String generatedAt) {
}

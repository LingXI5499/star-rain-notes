package com.starrainnotes.english.learning.vocabulary;

import java.util.List;

public record VocabularyQueueView(
        List<VocabularyStudyCardView> items,
        int dueCount,
        int newCount,
        String generatedAt) {
}

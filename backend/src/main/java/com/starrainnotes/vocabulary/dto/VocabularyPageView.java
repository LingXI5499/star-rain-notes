package com.starrainnotes.vocabulary.dto;

import java.util.List;

/**
 * Paged vocabulary list (manual LIMIT/OFFSET, matching the rest of V1).
 */
public record VocabularyPageView(
        List<VocabularyWordView> items,
        long total,
        int page,
        int pageSize,
        int totalPages) {
}

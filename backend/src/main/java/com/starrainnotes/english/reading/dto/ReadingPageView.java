package com.starrainnotes.english.reading.dto;

import java.util.List;

/**
 * Paged reading-article list response. {@code stats} aggregates counts for the
 * admin workbench summary (total / published / draft / per-CEFR / missing
 * exercise) so it is computed in one pass, not by N+1 queries.
 */
public record ReadingPageView(
        List<ReadingArticleSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages,
        ReadingAdminStats stats) {
}

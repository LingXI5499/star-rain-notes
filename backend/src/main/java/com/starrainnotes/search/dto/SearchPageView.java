package com.starrainnotes.search.dto;

import java.util.List;

/**
 * Paged search response: discriminated items, per-source counts (ignoring the
 * type filter), total (after the type filter) and pagination.
 */
public record SearchPageView(
        List<SearchItemView> items,
        SearchCountsView counts,
        long total,
        int page,
        int pageSize,
        int totalPages) {
}

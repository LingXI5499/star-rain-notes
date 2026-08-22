package com.starrainnotes.portfolio.dto;

import java.util.List;

/**
 * Admin portfolio project page.
 */
public record AdminProjectPageView(
        List<AdminProjectSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}

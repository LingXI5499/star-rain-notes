package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Admin tutorial list page (04 §3 envelope; never a raw IPage).
 */
public record TutorialPageView(
        List<AdminTutorialSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}

package com.starrainnotes.site.dto;

import java.util.List;

/**
 * GET /api/v1/admin/dashboard — content counts, draft counts and recent content.
 */
public record DashboardView(
        ContentCountsView contentCounts,
        DraftCountsView draftCounts,
        List<RecentContentView> recentContent) {
}

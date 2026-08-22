package com.starrainnotes.portfolio.dto;

/**
 * Public portfolio project list item (no pagination; featured first).
 */
public record PublicProjectSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String projectStatus,
        boolean featured,
        Integer sortOrder,
        String updatedAt) {
}

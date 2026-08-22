package com.starrainnotes.portfolio.dto;

/**
 * Admin portfolio project list item.
 */
public record AdminProjectSummaryView(
        Long id,
        String title,
        String slug,
        String publishStatus,
        String projectStatus,
        boolean featured,
        Integer sortOrder,
        String publishedAt,
        String updatedAt) {
}

package com.starrainnotes.portfolio.dto;

import java.util.List;

/**
 * Admin portfolio project list item.
 */
public record AdminProjectSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String role,
        List<String> techStack,
        String coverUrl,
        String publishStatus,
        String projectStatus,
        boolean featured,
        Integer sortOrder,
        String publishedAt,
        String updatedAt) {
}

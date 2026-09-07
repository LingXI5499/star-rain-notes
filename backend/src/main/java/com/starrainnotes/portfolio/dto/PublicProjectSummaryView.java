package com.starrainnotes.portfolio.dto;

/**
 * Public portfolio project list item (no pagination; featured first).
 */
public record PublicProjectSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String role,
        java.util.List<String> techStack,
        String coverUrl,
        String coverSrcSet,
        Integer coverWidth,
        Integer coverHeight,
        String projectStatus,
        boolean featured,
        Integer sortOrder,
        String updatedAt) {
}

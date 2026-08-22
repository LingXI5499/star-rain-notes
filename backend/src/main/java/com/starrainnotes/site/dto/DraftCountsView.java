package com.starrainnotes.site.dto;

/**
 * Dashboard draft counts — DRAFT rows per content type.
 */
public record DraftCountsView(
        long tutorials,
        long chapters,
        long blogPosts,
        long portfolioProjects) {
}

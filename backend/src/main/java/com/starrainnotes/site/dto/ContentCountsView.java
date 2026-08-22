package com.starrainnotes.site.dto;

/**
 * Dashboard content counts — no analytics / PV / UV / charts (04 §9).
 */
public record ContentCountsView(
        long tutorials,
        long chapters,
        long blogPosts,
        long portfolioProjects) {
}

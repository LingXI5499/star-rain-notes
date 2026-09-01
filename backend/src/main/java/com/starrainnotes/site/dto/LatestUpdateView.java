package com.starrainnotes.site.dto;

/**
 * One entry of the Home "Latest Updates" list.
 * type: TUTORIAL (a published chapter) | BLOG | PORTFOLIO.
 */
public record LatestUpdateView(
        String type,
        Long id,
        String title,
        String slug,
        String tutorialSlug,
        String chapterSlug,
        String activityAt) {
}

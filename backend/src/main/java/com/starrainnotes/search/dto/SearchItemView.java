package com.starrainnotes.search.dto;

/**
 * One search hit. `type` discriminates the source; type-specific slug fields
 * let the client build the public URL.
 */
public record SearchItemView(
        String type,
        Long id,
        String title,
        String summary,
        String slug,
        String tutorialSlug,
        String chapterSlug,
        String activityAt,
        int score) {
}

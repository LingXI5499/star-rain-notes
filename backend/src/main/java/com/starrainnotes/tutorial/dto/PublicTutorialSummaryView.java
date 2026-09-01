package com.starrainnotes.tutorial.dto;

/**
 * Public tutorial list item (published only).
 */
public record PublicTutorialSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        Long categoryId,
        String categoryName,
        long publishedChapterCount,
        String firstChapterSlug) {
}

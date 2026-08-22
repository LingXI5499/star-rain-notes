package com.starrainnotes.tutorial.dto;

/**
 * Admin tutorial list item.
 */
public record AdminTutorialSummaryView(
        Long id,
        String title,
        String slug,
        Long categoryId,
        String categoryName,
        String publishStatus,
        String publishedAt,
        String updatedAt) {
}

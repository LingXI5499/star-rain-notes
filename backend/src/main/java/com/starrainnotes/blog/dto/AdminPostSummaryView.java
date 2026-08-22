package com.starrainnotes.blog.dto;

/**
 * Blog post admin list item.
 */
public record AdminPostSummaryView(
        Long id,
        String title,
        String slug,
        String publishStatus,
        String publishedAt,
        String updatedAt) {
}

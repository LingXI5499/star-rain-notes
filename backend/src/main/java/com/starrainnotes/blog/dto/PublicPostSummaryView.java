package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Public blog timeline item.
 */
public record PublicPostSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String publishedAt,
        String updatedAt,
        List<PublicTagView> tags) {
}

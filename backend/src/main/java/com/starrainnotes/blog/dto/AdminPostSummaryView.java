package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Blog post admin list item.
 */
public record AdminPostSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String publishStatus,
        String publishedAt,
        String updatedAt,
        List<BlogTagView> tags) {
}

package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Admin blog post detail (entity never exposed; includes tags).
 */
public record AdminPostDetailView(
        Long id,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        Long coverMediaId,
        String coverUrl,
        String publishStatus,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String createdAt,
        String updatedAt,
        List<BlogTagView> tags) {
}

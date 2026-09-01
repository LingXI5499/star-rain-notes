package com.starrainnotes.tutorial.dto;

import java.time.LocalDateTime;

/**
 * Admin tutorial detail (entity never exposed; timestamps in site timezone).
 */
public record AdminTutorialDetailView(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        String slug,
        String summary,
        Long coverMediaId,
        String publishStatus,
        Integer sortOrder,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String createdAt,
        String updatedAt) {
}

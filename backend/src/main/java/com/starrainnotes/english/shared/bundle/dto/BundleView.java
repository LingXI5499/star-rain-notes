package com.starrainnotes.english.shared.bundle.dto;

public record BundleView(
        Long id,
        String title,
        String slug,
        String summary,
        String primaryCefr,
        Long coverMediaId,
        String coverUrl,
        String publishStatus,
        Integer sortOrder,
        String publishedAt,
        String updatedAt) {
}

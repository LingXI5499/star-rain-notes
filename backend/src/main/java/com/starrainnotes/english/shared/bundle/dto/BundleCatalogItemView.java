package com.starrainnotes.english.shared.bundle.dto;

public record BundleCatalogItemView(
        String contentType,
        Long contentId,
        String title,
        String slug,
        String summary,
        String cefrLevel,
        String coverUrl,
        String publishStatus,
        int sortOrder,
        boolean selected) {
}

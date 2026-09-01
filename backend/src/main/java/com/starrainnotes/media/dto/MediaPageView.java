package com.starrainnotes.media.dto;

import java.util.List;

/**
 * Paged media library response.
 */
public record MediaPageView(
        List<MediaAssetView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}

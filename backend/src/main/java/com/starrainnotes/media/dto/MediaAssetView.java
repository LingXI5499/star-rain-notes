package com.starrainnotes.media.dto;

import java.time.LocalDateTime;

/**
 * Media asset view (never exposes storage_path).
 */
public record MediaAssetView(
        Long id,
        String assetType,
        String originalName,
        String mimeType,
        String extension,
        Long sizeBytes,
        Integer width,
        Integer height,
        String publicUrl,
        String srcSet,
        LocalDateTime createdAt) {
}

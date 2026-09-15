package com.starrainnotes.portfolio.dto;

/** Metadata only; source files are never included in JSON responses. */
public record ProjectPrototypeView(
        Long mediaAssetId,
        String sourceName,
        long sizeBytes,
        String validationStatus,
        String revision,
        int fileCount,
        long totalBytes,
        String previewUrl,
        String publicEntryUrl) {
}

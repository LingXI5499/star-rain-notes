package com.starrainnotes.portfolio.dto;

/**
 * One gallery frame on a public/admin case study.
 */
public record ProjectMediaView(
        Long id,
        Long mediaAssetId,
        String url,
        String title,
        String description,
        String altText,
        String deviceType,
        Integer sortOrder,
        Integer width,
        Integer height,
        String srcSet) {
}

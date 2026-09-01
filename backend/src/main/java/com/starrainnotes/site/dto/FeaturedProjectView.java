package com.starrainnotes.site.dto;

/**
 * Home "Featured Projects" entry (Published + featured, max 3).
 */
public record FeaturedProjectView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String projectStatus) {
}

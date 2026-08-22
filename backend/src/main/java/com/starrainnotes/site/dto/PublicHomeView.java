package com.starrainnotes.site.dto;

import java.util.List;

/**
 * GET /api/v1/public/home — data sections for the public home page.
 * Returns empty collections when no published content exists.
 */
public record PublicHomeView(
        List<LatestUpdateView> latestUpdates,
        List<FeaturedProjectView> featuredProjects,
        AboutPreviewView aboutPreview) {
}

package com.starrainnotes.site.dto;

/**
 * GET /api/v1/public/site — public-facing configuration only.
 * Never exposes internal storage paths or media ids.
 */
public record PublicSiteView(
        String siteName,
        String tagline,
        String siteUrl,
        String footerText,
        String githubUrl,
        String defaultSeoDescription,
        String timezone,
        String logoUrl,
        String faviconUrl) {
}

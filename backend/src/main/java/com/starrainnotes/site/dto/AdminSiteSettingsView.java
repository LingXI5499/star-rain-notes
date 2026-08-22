package com.starrainnotes.site.dto;

/**
 * GET/PUT /api/v1/admin/site-settings — explicit admin DTO for the singleton.
 */
public record AdminSiteSettingsView(
        Integer id,
        String siteName,
        String tagline,
        String siteUrl,
        String footerText,
        String githubUrl,
        String defaultSeoDescription,
        String timezone,
        Long logoMediaId,
        Long faviconMediaId) {
}

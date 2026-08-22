package com.starrainnotes.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/site-settings request body.
 * logoMediaId / faviconMediaId, when set, must reference an IMAGE media asset.
 */
public record UpdateSiteSettingsRequest(
        @NotBlank @Size(max = 100) String siteName,
        @Size(max = 255) String tagline,
        @Size(max = 255) String siteUrl,
        @Size(max = 500) String footerText,
        @Size(max = 500) String githubUrl,
        @Size(max = 500) String defaultSeoDescription,
        @NotBlank @Size(max = 64) String timezone,
        Long logoMediaId,
        Long faviconMediaId) {
}

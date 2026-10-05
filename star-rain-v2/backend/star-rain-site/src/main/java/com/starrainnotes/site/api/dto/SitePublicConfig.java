package com.starrainnotes.site.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SitePublicConfig {
    private String siteName;
    private String siteTitle;
    private String tagline;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
    private String logoUrl;
    private String faviconUrl;
}

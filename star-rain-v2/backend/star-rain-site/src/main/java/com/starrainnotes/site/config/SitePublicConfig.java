package com.starrainnotes.site.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SitePublicConfig {
    private String siteName;
    private String siteTitle;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
    private String logoUrl;
    private String faviconUrl;
}

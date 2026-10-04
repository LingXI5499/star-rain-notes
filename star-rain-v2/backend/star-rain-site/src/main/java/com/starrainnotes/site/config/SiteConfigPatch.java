package com.starrainnotes.site.config;

import lombok.Data;

@Data
public class SiteConfigPatch {
    private String siteName;
    private String siteTitle;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
}

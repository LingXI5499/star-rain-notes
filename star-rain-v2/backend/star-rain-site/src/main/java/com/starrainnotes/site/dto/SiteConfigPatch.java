package com.starrainnotes.site.dto;

import lombok.Data;

@Data
public class SiteConfigPatch {
    private String siteName;
    private String siteTitle;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
}

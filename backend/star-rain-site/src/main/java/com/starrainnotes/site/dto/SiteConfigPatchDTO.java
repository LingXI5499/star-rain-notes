package com.starrainnotes.site.dto;

import lombok.Data;

@Data
public class SiteConfigPatchDTO {
    private String siteName;
    private String siteTitle;
    private String tagline;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
}

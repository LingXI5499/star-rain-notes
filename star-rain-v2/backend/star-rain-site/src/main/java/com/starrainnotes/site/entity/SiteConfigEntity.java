package com.starrainnotes.site.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SiteConfigEntity {
    private Long id;
    private String configKey;
    private String siteName;
    private String siteTitle;
    private String tagline;
    private String siteDescription;
    private String homeIntro;
    private String footerText;
    private Long logoMediaAssetId;
    private Long faviconMediaAssetId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

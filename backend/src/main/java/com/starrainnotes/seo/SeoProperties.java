package com.starrainnotes.seo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.seo")
public record SeoProperties(
        String siteOrigin,
        String frontendIndexPath,
        String defaultShareImage,
        boolean indexNowEnabled,
        String indexNowKey,
        boolean baiduEnabled,
        String baiduToken,
        String baiduSite) {

    public SeoProperties {
        siteOrigin = cleanOrigin(siteOrigin);
        defaultShareImage = blank(defaultShareImage) ? "/og-default.svg" : defaultShareImage.trim();
        baiduSite = blank(baiduSite) ? siteOrigin : cleanOrigin(baiduSite);
    }

    private static String cleanOrigin(String value) {
        String origin = blank(value) ? "https://yulanlin.cn" : value.trim();
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }

    static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

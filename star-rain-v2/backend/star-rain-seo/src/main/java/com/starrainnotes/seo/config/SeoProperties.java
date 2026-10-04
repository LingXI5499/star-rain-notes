package com.starrainnotes.seo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "star-rain.seo")
public class SeoProperties {
    private String publicBaseUrl;
    private String frontendIndexPath;
    private IndexNow indexnow = new IndexNow();

    @Data
    public static class IndexNow {
        private boolean enabled;
        private String endpoint;
        private String key;
    }
}

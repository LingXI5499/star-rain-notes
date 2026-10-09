package com.starrainnotes.analytics.utils;

import com.starrainnotes.analytics.dto.ReferrerClassification;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReferrerClassifier {
    private static final Set<String> SEARCH = Set.of("google.com", "bing.com", "baidu.com", "sogou.com", "so.com");
    private static final Set<String> SOCIAL = Set.of("weibo.com", "zhihu.com", "x.com", "twitter.com", "facebook.com", "linkedin.com");

    @Value("${star-rain.account.frontend-origin:}")
    private String frontendOrigin;

    public ReferrerClassification classify(String referrer) {
        if (referrer == null || referrer.isBlank() || referrer.length() > 2048) {
            return new ReferrerClassification("DIRECT", null);
        }
        try {
            URI uri = URI.create(referrer);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return new ReferrerClassification("DIRECT", null);
            }
            String host = uri.getHost();
            if (host == null || host.length() > 255) {
                return new ReferrerClassification("DIRECT", null);
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals(configuredHost())) {
                return new ReferrerClassification("INTERNAL", host);
            }
            for (String domain : SEARCH) {
                if (host.equals(domain) || host.endsWith("." + domain)) return new ReferrerClassification("SEARCH", host);
            }
            for (String domain : SOCIAL) {
                if (host.equals(domain) || host.endsWith("." + domain)) return new ReferrerClassification("SOCIAL", host);
            }
            return new ReferrerClassification("OTHER", host);
        } catch (IllegalArgumentException ignored) {
            return new ReferrerClassification("DIRECT", null);
        }
    }

    private String configuredHost() {
        try {
            String host = frontendOrigin == null ? null : URI.create(frontendOrigin).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        }
        catch (IllegalArgumentException ignored) { return ""; }
    }
}

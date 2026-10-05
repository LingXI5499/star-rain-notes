package com.starrainnotes.seo.service;

import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.exception.SeoRouteInvalidException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CanonicalService {
    private final SeoProperties properties;

    public String baseUrl() {
        String raw = properties.getPublicBaseUrl();
        if (raw == null || raw.isBlank()) throw new IllegalStateException("SEO public base URL is required");
        URI uri = URI.create(raw.strip());
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
            || uri.getFragment() != null || (uri.getPath() != null && !uri.getPath().isBlank()
            && !"/".equals(uri.getPath()))) {
            throw new IllegalStateException("SEO public base URL is invalid");
        }
        return raw.strip().replaceAll("/+$", "");
    }

    public String canonical(String routePath) {
        if (routePath == null || !routePath.startsWith("/") || routePath.startsWith("//")
            || routePath.contains("?") || routePath.contains("#") || routePath.contains("\\")
            || routePath.contains("..") || routePath.length() > 500
            || routePath.chars().anyMatch(ch -> ch < 32 || ch == 127)) {
            throw new SeoRouteInvalidException();
        }
        return baseUrl() + routePath;
    }
}

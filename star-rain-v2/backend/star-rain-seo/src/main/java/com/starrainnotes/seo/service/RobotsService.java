package com.starrainnotes.seo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RobotsService {
    private final CanonicalService canonical;

    public String text() {
        return "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /useradmin/\n"
            + "Disallow: /search\nSitemap: " + canonical.baseUrl() + "/sitemap.xml\n";
    }
}

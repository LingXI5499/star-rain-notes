package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.service.RobotsService;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RobotsServiceImpl implements RobotsService {
    private final CanonicalUrlResolver canonical;

    @Override
    public String robotsTxt() {
        return "User-agent: *\n"
            + "Allow: /\n"
            + "Disallow: /api/\n"
            + "Disallow: /useradmin/\n"
            + "Disallow: /search\n"
            + "Sitemap: " + canonical.baseUrl() + "/sitemap.xml\n";
    }
}

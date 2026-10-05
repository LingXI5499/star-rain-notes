package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.service.CanonicalService;
import com.starrainnotes.seo.service.RobotsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RobotsServiceImpl implements RobotsService {
    private final CanonicalService canonical;

    @Override
    public String text() {
        return "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /useradmin/\n"
            + "Disallow: /search\nSitemap: " + canonical.baseUrl() + "/sitemap.xml\n";
    }
}

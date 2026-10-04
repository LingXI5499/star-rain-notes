package com.starrainnotes.seo.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SeoSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "seo"; }
    @Override public int order() { return 48; }
    @Override public List<String> publicPatterns() {
        return List.of("/sitemap.xml", "/robots.txt", "/api/public/seo/**", "/",
            "/blog", "/blog/**", "/tutorials", "/tutorials/**",
            "/portfolio", "/portfolio/**", "/messages", "/about");
    }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/seo/**"); }
}

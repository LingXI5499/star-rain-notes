package com.starrainnotes.site.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SiteSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "site"; }
    @Override public int order() { return 47; }
    @Override public List<String> publicPatterns() { return List.of("/api/public/site/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/site/**"); }
}

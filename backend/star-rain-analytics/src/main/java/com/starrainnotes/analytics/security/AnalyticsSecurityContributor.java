package com.starrainnotes.analytics.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "analytics"; }
    @Override public int order() { return 48; }
    @Override public List<String> publicPatterns() { return List.of("/api/public/analytics/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/analytics/**"); }
}

package com.starrainnotes.search.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SearchSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "search"; }
    @Override public int order() { return 49; }
    @Override public List<String> publicPatterns() { return List.of("/api/public/search", "/api/public/search/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/search/**"); }
}

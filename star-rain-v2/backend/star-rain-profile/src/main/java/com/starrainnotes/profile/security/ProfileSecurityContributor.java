package com.starrainnotes.profile.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProfileSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "profile"; }
    @Override public int order() { return 47; }
    @Override public List<String> publicPatterns() { return List.of("/api/public/profile/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/profile/**"); }
}

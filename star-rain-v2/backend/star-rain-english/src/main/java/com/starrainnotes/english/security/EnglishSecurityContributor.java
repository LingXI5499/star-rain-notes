package com.starrainnotes.english.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnglishSecurityContributor implements ModuleSecurityContributor {
    @Override public String moduleName() { return "english"; }
    @Override public int order() { return 48; }
    @Override public List<String> publicPatterns() { return List.of("/api/public/english/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/english/**", "/api/account/english/writing/**"); }
}

package com.starrainnotes.boot.reliability;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ManagementSecurity implements ModuleSecurityContributor {
    @Override public String moduleName() { return "management"; }
    @Override public int order() { return 80; }
    @Override public List<String> publicPatterns() { return List.of("/actuator/health", "/actuator/health/**"); }
    @Override public List<String> authenticatedPatterns() { return List.of("/actuator/metrics", "/actuator/metrics/**"); }
}

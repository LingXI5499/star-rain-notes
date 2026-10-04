package com.starrainnotes.portfolio.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PortfolioSecurityContributor implements ModuleSecurityContributor {
    @Override
    public String moduleName() {
        return "portfolio";
    }

    @Override
    public int order() {
        return 45;
    }

    @Override
    public List<String> publicPatterns() {
        return List.of("/api/public/portfolio/**");
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/portfolio/**");
    }
}

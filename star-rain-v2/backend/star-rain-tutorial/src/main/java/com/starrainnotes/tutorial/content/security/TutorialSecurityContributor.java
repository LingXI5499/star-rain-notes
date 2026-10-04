package com.starrainnotes.tutorial.content.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TutorialSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleName() {
        return "tutorial";
    }

    @Override
    public int order() {
        return 35;
    }

    @Override
    public List<String> publicPatterns() {
        return List.of("/api/public/tutorials/**", "/api/public/tutorials",
                "/api/public/tutorial-categories/**");
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/tutorials/**", "/api/admin/tutorials",
                "/api/admin/tutorial-categories", "/api/admin/tutorial-categories/**",
                "/api/admin/tutorial-groups/**", "/api/admin/tutorial-chapters/**",
                "/api/admin/tutorial-cards/**", "/api/admin/tutorial-questions/**");
    }
}

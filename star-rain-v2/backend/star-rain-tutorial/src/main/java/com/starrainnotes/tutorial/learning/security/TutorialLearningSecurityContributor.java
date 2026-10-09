package com.starrainnotes.tutorial.learning.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TutorialLearningSecurityContributor implements ModuleSecurityContributor {
    @Override
    public String moduleName() {
        return "tutorial-learning";
    }

    @Override
    public int order() {
        return 36;
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/account/learning/**", "/api/account/learning");
    }
}

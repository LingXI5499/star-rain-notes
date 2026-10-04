package com.starrainnotes.message.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MessageSecurityContributor implements ModuleSecurityContributor {
    @Override
    public String moduleName() {
        return "message";
    }

    @Override
    public int order() {
        return 46;
    }

    @Override
    public List<String> publicPatterns() {
        return List.of("/api/public/messages/**");
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/messages/**");
    }
}

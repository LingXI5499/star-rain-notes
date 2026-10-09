package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Keep DENY as the site default; only module-declared static previews allow their own frame. */
public final class ModuleFrameHeaders {
    private ModuleFrameHeaders() { }

    public static HeaderWriter writer(List<ModuleSecurityContributor> contributors) {
        List<RequestMatcher> previews = contributors.stream()
                .flatMap(module -> module.sameOriginFramePatterns().stream())
                .map(pattern -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(pattern))
                .toList();
        return (request, response) -> response.setHeader("X-Frame-Options",
                previews.stream().anyMatch(matcher -> matcher.matches(request)) ? "SAMEORIGIN" : "DENY");
    }
}

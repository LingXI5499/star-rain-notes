package com.starrainnotes.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ModuleFrameHeadersTest {
    @Test
    void onlyTheDeclaredPrototypePathCanBeFramed() {
        var module = new ModuleSecurityContributor() {
            public String moduleName() { return "portfolio"; }
            public List<String> sameOriginFramePatterns() { return List.of("/api/public/portfolio/works/*/live/**"); }
        };
        var writer = ModuleFrameHeaders.writer(List.of(module));
        for (String path : List.of("/api/public/portfolio/works/demo/live/index.html",
                "/api/public/portfolio/works/demo/live/assets/app.js")) {
            var response = new MockHttpServletResponse();
            writer.writeHeaders(new MockHttpServletRequest("GET", path), response);
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        }
        for (String path : List.of("/api/admin/accounts", "/api/public/portfolio/works/demo",
                "/api/public/portfolio/works/demo/live-other/index.html", "/useradmin/login")) {
            var response = new MockHttpServletResponse();
            writer.writeHeaders(new MockHttpServletRequest("GET", path), response);
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        }
    }

    @Test
    void noContributorKeepsDenyEverywhere() {
        var response = new MockHttpServletResponse();
        ModuleFrameHeaders.writer(List.of()).writeHeaders(new MockHttpServletRequest("GET", "/index.html"), response);
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }
}

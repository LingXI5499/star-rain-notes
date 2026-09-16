package com.starrainnotes.portfolio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Mirrors the production Nginx headers while Spring serves uploads locally. */
@Component
public class PrototypeSecurityHeadersFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean publicPrototype = uri.startsWith("/uploads/prototypes/");
        boolean adminPreview = uri.startsWith("/api/v1/admin/portfolio/projects/")
                && uri.contains("/prototype-preview/");
        if (publicPrototype || adminPreview) {
            response.setHeader("Content-Security-Policy", "default-src 'self' data:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'self'");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", publicPrototype
                    ? "public, max-age=31536000, immutable"
                    : "no-store");
        }
        chain.doFilter(request, response);
    }
}

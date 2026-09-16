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
        if (request.getRequestURI().startsWith("/uploads/prototypes/")) {
            response.setHeader("Content-Security-Policy", "default-src 'self' data:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        }
        chain.doFilter(request, response);
    }
}

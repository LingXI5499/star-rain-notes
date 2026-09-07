package com.starrainnotes.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Short browser/CDN caching for anonymous public GET APIs.
 * Skips anything under /api/v1/admin and non-GET methods.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class PublicApiCacheHeadersFilter extends OncePerRequestFilter {

    private static final String PUBLIC_CACHE = "public, max-age=30, stale-while-revalidate=60";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/public/")) {
            return;
        }
        // Auth/session-sensitive public endpoints must stay private.
        if (path.contains("/account") || path.contains("/learning") || path.contains("/vocabulary/memory")) {
            return;
        }
        if (response.getHeader("Cache-Control") == null && response.getStatus() >= 200 && response.getStatus() < 400) {
            response.setHeader("Cache-Control", PUBLIC_CACHE);
        }
    }
}

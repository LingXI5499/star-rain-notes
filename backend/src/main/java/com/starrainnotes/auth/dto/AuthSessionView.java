package com.starrainnotes.auth.dto;

import java.util.List;

/**
 * GET /api/v1/auth/session and POST /api/v1/auth/login response.
 * Anonymous requests return {@code {authenticated: false}}.
 */
public record AuthSessionView(boolean authenticated, String username, String role, List<String> capabilities) {

    public static AuthSessionView anonymous() {
        return new AuthSessionView(false, null, null, List.of());
    }

    public static AuthSessionView authenticated(String username, String role) {
        return new AuthSessionView(true, username, role, capabilities(role));
    }

    private static List<String> capabilities(String role) {
        if ("ROLE_SUPER_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE",
                    "SUPER_ADMIN", "REVIEW");
        }
        if ("ROLE_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE");
        }
        return List.of();
    }
}

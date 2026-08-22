package com.starrainnotes.auth.dto;

/**
 * GET /api/v1/auth/session and POST /api/v1/auth/login response.
 * Anonymous requests return {@code {authenticated: false}}.
 */
public record AuthSessionView(boolean authenticated, String username, String role) {

    public static AuthSessionView anonymous() {
        return new AuthSessionView(false, null, null);
    }

    public static AuthSessionView authenticated(String username, String role) {
        return new AuthSessionView(true, username, role);
    }
}

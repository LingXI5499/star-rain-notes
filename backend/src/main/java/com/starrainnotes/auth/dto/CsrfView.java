package com.starrainnotes.auth.dto;

/**
 * GET /api/v1/auth/csrf response. The token is also set as the
 * (non-HttpOnly) XSRF-TOKEN cookie so the SPA can echo it in X-XSRF-TOKEN.
 */
public record CsrfView(String csrfToken) {
}

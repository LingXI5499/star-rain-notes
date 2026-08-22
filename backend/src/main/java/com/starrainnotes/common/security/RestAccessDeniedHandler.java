package com.starrainnotes.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security filter-chain 403 → RFC 9457 Problem Details.
 *
 * <p>CSRF failures (CsrfException) map to {@code CSRF_INVALID} so the SPA can
 * refresh its token once; every other forbidden access maps to
 * {@code FORBIDDEN}. Registered both as the CSRF access-denied handler and as
 * the general access-denied handler.</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        boolean csrfFailure = accessDeniedException instanceof CsrfException;
        var problem = ApiProblem.create(
                HttpStatus.FORBIDDEN,
                csrfFailure ? "CSRF_INVALID" : "FORBIDDEN",
                csrfFailure ? "Invalid or missing CSRF token" : "Forbidden",
                csrfFailure
                        ? "A valid CSRF token is required for this request."
                        : "You do not have permission to perform this action.",
                request);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}

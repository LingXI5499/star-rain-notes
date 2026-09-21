package com.starrainnotes.account.security;

import com.starrainnotes.account.service.AccountQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Invalidates an account session immediately after disablement or credential changes. */
public final class AccountSessionValidationFilter extends OncePerRequestFilter {

    private final AccountQueryService accountQueries;

    public AccountSessionValidationFilter(AccountQueryService accountQueries) {
        this.accountQueries = accountQueries;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                && !accountQueries.isSessionValid(principal.getId(), principal.getAuthVersion())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        filterChain.doFilter(request, response);
    }
}

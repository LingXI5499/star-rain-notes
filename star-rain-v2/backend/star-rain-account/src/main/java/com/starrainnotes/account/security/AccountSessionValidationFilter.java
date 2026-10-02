package com.starrainnotes.account.security;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionValidationFilter extends OncePerRequestFilter {

    private final AccountMapper mapper;

    public AccountSessionValidationFilter(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            AccountEntity account = mapper.accountById(principal.accountId());
            if (account == null || !"ACTIVE".equals(account.getStatus())
                    || account.getAuthVersion() == null || account.getAuthVersion() != principal.authVersion()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}


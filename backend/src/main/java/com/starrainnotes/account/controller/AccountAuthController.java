package com.starrainnotes.account.controller;

import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.dto.AccountLoginRequest;
import com.starrainnotes.account.dto.AccountSessionView;
import com.starrainnotes.account.dto.ChangePasswordRequest;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/account")
public class AccountAuthController {

    private final AccountService accountService;
    private final SecurityContextRepository securityContextRepository;
    private final AuditLogService auditLog;

    public AccountAuthController(AccountService accountService, SecurityContextRepository securityContextRepository,
                                 AuditLogService auditLog) {
        this.accountService = accountService;
        this.securityContextRepository = securityContextRepository;
        this.auditLog = auditLog;
    }

    @PostMapping("/login")
    public AccountSessionView login(@RequestBody AccountLoginRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        AccountUser user;
        try {
            user = accountService.authenticate(body.email(), body.password());
        } catch (RuntimeException ex) {
            auditLog.record(null, "LOGIN", "ACCOUNT", null, "FAILURE", clientIp(request),
                    request.getHeader("User-Agent"), null);
            throw ex;
        }
        auditLog.record(user.getId(), "LOGIN", "ACCOUNT", user.getId(), "SUCCESS",
                clientIp(request), request.getHeader("User-Agent"), null);
        AccountPrincipal principal = new AccountPrincipal(user.getId(), user.getEmail(), user.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null,
                principal.authorities().stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return new AccountSessionView(true, user.getEmail(), user.getRole(), user.getAccountStatus(),
                accountService.capabilities(user.getRole()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        accountService.changePassword(body.email(), body.currentPassword(), body.newPassword());
        auditLog.record(accountUserIdOrNull(body.email()), "CHANGE_PASSWORD", "ACCOUNT", null, "SUCCESS",
                clientIp(request), request.getHeader("User-Agent"), null);
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private Long accountUserIdOrNull(String email) {
        try {
            return accountService.findByEmailPublic(email).getId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
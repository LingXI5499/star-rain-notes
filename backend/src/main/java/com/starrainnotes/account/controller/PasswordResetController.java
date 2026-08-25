package com.starrainnotes.account.controller;

import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.dto.PasswordResetRequest;
import com.starrainnotes.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {

    private final AccountService accountService;
    private final AuditLogService auditLogService;

    public PasswordResetController(AccountService accountService, AuditLogService auditLogService) {
        this.accountService = accountService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(@RequestBody PasswordResetRequest.EmailOnly body, HttpServletRequest request) {
        accountService.requestPasswordReset(body.email(), AccountActivationController.clientIp(request));
        auditLogService.record(null, "PASSWORD_RESET_CODE", "ACCOUNT", null, "SUCCESS",
                AccountActivationController.clientIp(request), request.getHeader("User-Agent"), null);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody PasswordResetRequest body, HttpServletRequest request) {
        accountService.confirmPasswordReset(body.email(), body.verificationCode(), body.newPassword());
        auditLogService.record(null, "PASSWORD_RESET_CONFIRMED", "ACCOUNT", null, "SUCCESS",
                AccountActivationController.clientIp(request), request.getHeader("User-Agent"), null);
    }
}

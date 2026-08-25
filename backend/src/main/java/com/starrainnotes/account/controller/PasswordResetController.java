package com.starrainnotes.account.controller;

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

    public PasswordResetController(AccountService accountService) { this.accountService = accountService; }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(@RequestBody PasswordResetRequest.EmailOnly body, HttpServletRequest request) {
        accountService.requestPasswordReset(body.email(), AccountActivationController.clientIp(request));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody PasswordResetRequest body) {
        accountService.confirmPasswordReset(body.email(), body.verificationCode(), body.newPassword());
    }
}
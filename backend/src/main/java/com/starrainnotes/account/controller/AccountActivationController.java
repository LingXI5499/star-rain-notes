package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.ActivationConfirmRequest;
import com.starrainnotes.account.dto.ActivationStatusView;
import com.starrainnotes.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/super-admin-activation")
public class AccountActivationController {

    private final AccountService accountService;

    public AccountActivationController(AccountService accountService) { this.accountService = accountService; }

    @GetMapping("/status")
    public ActivationStatusView status() {
        String email = accountService.configuredSuperAdminEmail();
        boolean configured = email != null && !email.isBlank() && AccountService.validEmail(email);
        return new ActivationStatusView(configured, accountService.superAdminActivated(),
                AccountService.mask(email));
    }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(HttpServletRequest request) {
        accountService.requestActivationCode(clientIp(request));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody ActivationConfirmRequest body) {
        accountService.confirmActivation(body.verificationCode(), body.password());
    }

    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.AccountLoginRequest;
import com.starrainnotes.account.dto.AccountSessionView;
import com.starrainnotes.account.dto.ChangePasswordRequest;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public AccountAuthController(AccountService accountService) { this.accountService = accountService; }

    @PostMapping("/login")
    public AccountSessionView login(@RequestBody AccountLoginRequest body) {
        AccountUser user = accountService.authenticate(body.email(), body.password());
        return new AccountSessionView(true, user.getEmail(), user.getRole(), user.getAccountStatus(),
                accountService.capabilities(user.getRole()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        accountService.changePassword(body.email(), body.currentPassword(), body.newPassword());
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
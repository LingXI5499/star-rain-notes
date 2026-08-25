package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.InvitationActionRequest;
import com.starrainnotes.account.entity.AdminInvitation;
import com.starrainnotes.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super-admin operations: invite resend/revoke + account disable/enable. */
@RestController
@RequestMapping("/api/v1/super-admin")
public class SuperAdminAccountController {

    private final AccountService accountService;

    public SuperAdminAccountController(AccountService accountService) { this.accountService = accountService; }

    @GetMapping("/invitations")
    public List<AdminInvitation> invitations() {
        return accountService.listInvitations();
    }

    @PostMapping("/invitations/{id}/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@PathVariable Long id, @RequestHeader(value = "X-Actor-Account", required = false) Long actor) {
        accountService.requireSuperAdminOr401(actor);
        accountService.resendInvitation(id);
    }

    @PostMapping("/invitations/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, @RequestHeader(value = "X-Actor-Account", required = false) Long actor) {
        accountService.requireSuperAdminOr401(actor);
        accountService.revokeInvitation(id);
    }

    @PostMapping("/users/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id, @RequestBody(required = false) String reason,
                        @RequestHeader(value = "X-Actor-Account", required = false) Long actor) {
        accountService.requireSuperAdminOr401(actor);
        accountService.disable(id, reason == null || reason.isBlank() ? null : reason, actor);
    }

    @PostMapping("/users/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id, @RequestHeader(value = "X-Actor-Account", required = false) Long actor) {
        accountService.requireSuperAdminOr401(actor);
        accountService.enable(id);
    }
}
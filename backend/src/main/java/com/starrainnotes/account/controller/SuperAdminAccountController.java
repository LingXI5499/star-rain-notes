package com.starrainnotes.account.controller;

import com.starrainnotes.account.audit.AuditLog;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.dto.AccountUserView;
import com.starrainnotes.account.dto.AdminInvitationView;
import com.starrainnotes.account.dto.DisableAccountRequest;
import com.starrainnotes.account.dto.InviteRequest;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super-admin operations: users, invitations and audit history. */
@RestController
@RequestMapping("/api/v1/super-admin")
public class SuperAdminAccountController {

    private final AccountService accountService;
    private final AuditLogService auditLogService;

    public SuperAdminAccountController(AccountService accountService, AuditLogService auditLogService) {
        this.accountService = accountService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public List<AccountUserView> users() {
        return accountService.listUsers().stream().map(AccountUserView::from).toList();
    }

    @GetMapping("/invitations")
    public List<AdminInvitationView> invitations() {
        return accountService.listInvitations().stream().map(AdminInvitationView::from).toList();
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminInvitationView invite(@Valid @RequestBody InviteRequest body, Authentication authentication) {
        var issued = accountService.createInvitation(body.email(), actorId(authentication));
        return AdminInvitationView.from(issued.invitation(), issued.inviteLink());
    }

    @PostMapping("/invitations/{id}/resend")
    public AdminInvitationView resend(@PathVariable Long id) {
        var issued = accountService.resendInvitation(id);
        return AdminInvitationView.from(issued.invitation(), issued.inviteLink());
    }

    @PostMapping("/invitations/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, Authentication authentication) {
        accountService.revokeInvitation(id, actorId(authentication));
    }

    @DeleteMapping("/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@PathVariable Long id, Authentication authentication) {
        accountService.deleteInvitation(id, actorId(authentication));
    }

    @PostMapping("/users/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id, @Valid @RequestBody(required = false) DisableAccountRequest body,
                        Authentication authentication) {
        String reason = body == null || body.reason() == null || body.reason().isBlank() ? null : body.reason();
        accountService.disable(id, reason, actorId(authentication));
    }

    @PostMapping("/users/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id, Authentication authentication) {
        accountService.enable(id, actorId(authentication));
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize,
                                    @RequestParam(required = false) String action) {
        return auditLogService.list(page, pageSize, action);
    }

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}

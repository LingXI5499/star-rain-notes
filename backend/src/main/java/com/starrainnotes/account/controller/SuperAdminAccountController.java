package com.starrainnotes.account.controller;

import com.starrainnotes.account.audit.AuditLog;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.dto.AccountUserView;
import com.starrainnotes.account.dto.AdminInvitationView;
import com.starrainnotes.account.dto.DisableAccountRequest;
import com.starrainnotes.account.dto.InviteRequest;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.account.service.AccountAdministrationService;
import com.starrainnotes.account.service.AccountInvitationService;
import com.starrainnotes.account.service.AccountQueryService;
import com.starrainnotes.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AccountAdministrationService administration;
    private final AccountInvitationService invitations;
    private final AccountQueryService accountQueries;
    private final AuditLogService auditLogService;

    public SuperAdminAccountController(AccountService accountService, AccountAdministrationService administration,
                                       AccountInvitationService invitations, AccountQueryService accountQueries,
                                       AuditLogService auditLogService) {
        this.accountService = accountService;
        this.administration = administration;
        this.invitations = invitations;
        this.accountQueries = accountQueries;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public List<AccountUserView> users() {
        return accountQueries.listUsers().stream().map(AccountUserView::from).toList();
    }

    @GetMapping("/invitations")
    public List<AdminInvitationView> invitations() {
        return invitations.list().stream().map(AdminInvitationView::from).toList();
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminInvitationView invite(@Valid @RequestBody InviteRequest body, @AuthenticationPrincipal AccountPrincipal principal) {
        var issued = invitations.create(body.email(), actorId(principal));
        return AdminInvitationView.from(issued.invitation(), issued.inviteLink());
    }

    @PostMapping("/invitations/{id}/resend")
    public AdminInvitationView resend(@PathVariable Long id) {
        var issued = invitations.resend(id);
        return AdminInvitationView.from(issued.invitation(), issued.inviteLink());
    }

    @PostMapping("/invitations/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        invitations.revoke(id, actorId(principal));
    }

    @DeleteMapping("/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        invitations.delete(id, actorId(principal));
    }

    @PostMapping("/users/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id, @Valid @RequestBody(required = false) DisableAccountRequest body,
                        @AuthenticationPrincipal AccountPrincipal principal) {
        String reason = body == null || body.reason() == null || body.reason().isBlank() ? null : body.reason();
        administration.disable(id, reason, actorId(principal));
    }

    @PostMapping("/users/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        administration.enable(id, actorId(principal));
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize,
                                    @RequestParam(required = false) String action) {
        return auditLogService.list(page, pageSize, action);
    }

    private Long actorId(AccountPrincipal principal) {
        return principal == null ? null : principal.getId();
    }
}

package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.CreateAdminInvitationDTO;
import com.starrainnotes.account.dto.AccountStatusDTO;
import com.starrainnotes.account.vo.AdminAccountVO;
import com.starrainnotes.account.vo.AccountAuditVO;
import com.starrainnotes.account.vo.AdminInvitationVO;
import com.starrainnotes.account.vo.InvitationRecordVO;
import com.starrainnotes.account.service.AdminAccountService;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:read')")
    public ApiResponse<PageResult<AdminAccountVO>> accounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.accounts(page, pageSize, keyword, status));
    }

    @PatchMapping("/accounts/{accountId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:disable')")
    public ApiResponse<Void> changeStatus(@PathVariable String accountId,
                                          @Valid @RequestBody AccountStatusDTO request) {
        service.changeStatus(accountId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/account-invitations")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<AdminInvitationVO> createInvitation(
            @Valid @RequestBody CreateAdminInvitationDTO request) {
        return ApiResponse.ok(service.createInvitation(request));
    }

    @GetMapping("/account-invitations")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<PageResult<InvitationRecordVO>> invitations(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.invitations(page, pageSize));
    }

    @PostMapping("/account-invitations/{invitationId}/resend")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<AdminInvitationVO> resendInvitation(@PathVariable String invitationId) {
        return ApiResponse.ok(service.resendInvitation(invitationId));
    }

    @PostMapping("/account-invitations/{invitationId}/revoke")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<Void> revokeInvitation(@PathVariable String invitationId) {
        service.revokeInvitation(invitationId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/accounts/{accountId}/revoke-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:revoke-admin')")
    public ApiResponse<Void> removeAdministrator(@PathVariable String accountId) {
        service.removeAdministrator(accountId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/account-audits")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:audit-read')")
    public ApiResponse<PageResult<AccountAuditVO>> audits(
            @RequestParam(required = false) Long actorAccountId,
            @RequestParam(required = false) Long targetAccountId,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.audits(actorAccountId, targetAccountId, actionCode,
                result, startTime, endTime, page, pageSize));
    }
}

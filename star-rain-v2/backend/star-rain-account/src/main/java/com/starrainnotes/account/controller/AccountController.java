package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.UpdateMyAccountDTO;
import com.starrainnotes.account.dto.ConfirmEmailDTO;
import com.starrainnotes.account.vo.EmailVerificationCodeVO;
import com.starrainnotes.account.service.EmailVerificationService;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.account.dto.ChangePasswordDTO;
import com.starrainnotes.account.dto.AcceptAdminInvitationDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.vo.InvitationRecordVO;
import java.util.List;
import com.starrainnotes.account.service.AccountAuthService;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.account.service.AdminAccountService;
import com.starrainnotes.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountIdentityService identity;
    private final AccountAuthService auth;
    private final AdminAccountService admin;
    private final EmailVerificationService verification;

    public AccountController(AccountIdentityService identity, AccountAuthService auth,
                             AdminAccountService admin, EmailVerificationService verification) {
        this.identity = identity;
        this.auth = auth;
        this.admin = admin;
        this.verification = verification;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentAccountVO> me() {
        return ApiResponse.ok(identity.currentView());
    }

    @PatchMapping("/me")
    public ApiResponse<CurrentAccountVO> updateMe(@Valid @RequestBody UpdateMyAccountDTO request) {
        return ApiResponse.ok(auth.updateMe(request));
    }

    @PostMapping("/me/email/verification-codes")
    public ApiResponse<EmailVerificationCodeVO> sendEmailCode(HttpServletRequest request) {
        // 发信失败的审计由 EmailVerificationService 写到独立事务里（AccountAuditService.failedIndependently），
        // 因此接入层不再需要 try/catch —— 那条规则已经回到服务层自己身上。
        return ApiResponse.ok(verification.sendForCurrentAccount(request.getRemoteAddr()));
    }

    @PostMapping("/me/email/confirm")
    public ApiResponse<CurrentAccountVO> confirmEmail(@Valid @RequestBody ConfirmEmailDTO request) {
        return ApiResponse.ok(auth.confirmEmail(request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request,
                                            HttpServletRequest servletRequest) {
        auth.changePassword(request);
        if (servletRequest.getSession(false) != null) {
            servletRequest.getSession(false).invalidate();
        }
        return ApiResponse.ok(null);
    }

    @PostMapping("/invitations/accept")
    public ApiResponse<Void> acceptInvitation(@Valid @RequestBody AcceptAdminInvitationDTO request) {
        admin.acceptInvitation(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/invitations")
    public ApiResponse<List<InvitationRecordVO>> myInvitations() {
        return ApiResponse.ok(admin.myInvitations());
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ApiResponse<Void> acceptInvitationById(@PathVariable String invitationId) {
        admin.acceptInvitationById(invitationId);
        return ApiResponse.ok(null);
    }
}


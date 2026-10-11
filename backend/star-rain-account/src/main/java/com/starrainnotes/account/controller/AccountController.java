/*
 * AccountController：账号模块的HTTP 接入文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：UpdateMyAccountDTO、ConfirmEmailDTO、EmailVerificationCodeVO、EmailVerificationService、ApiException、ChangePasswordDTO、AcceptAdminInvitationDTO。
 * 阅读入口：me（me处理入口）；updateMe（更新Me）；sendEmailCode（发送邮箱代码）；confirmEmail（确认邮箱）。
 */

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

/**
 * 账号控制器，属于账号模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    /**
     * 身份。
     * 协作类型：<code>AccountIdentityService</code>。
     */
    private final AccountIdentityService identity;
    /**
     * 认证。
     * 协作类型：<code>AccountAuthService</code>。
     */
    private final AccountAuthService auth;
    /**
     * 后台。
     * 协作类型：<code>AdminAccountService</code>。
     */
    private final AdminAccountService admin;
    /**
     * 验证。
     * 协作类型：<code>EmailVerificationService</code>。
     */
    private final EmailVerificationService verification;

    /**
     * 构造AccountController，接收本类使用的依赖或初始化参数。
     *
     * @param identity 身份；类型为 <code>AccountIdentityService</code>
     * @param auth 认证；类型为 <code>AccountAuthService</code>
     * @param admin 后台；类型为 <code>AdminAccountService</code>
     * @param verification 验证；类型为 <code>EmailVerificationService</code>
     */
    public AccountController(AccountIdentityService identity, AccountAuthService auth,
                             AdminAccountService admin, EmailVerificationService verification) {
        this.identity = identity;
        this.auth = auth;
        this.admin = admin;
        this.verification = verification;
    }

    /**
     * me处理入口。
     * HTTP 映射：<code>@GetMapping("/me")</code>。
     * 主要协作调用：<code>identity.currentView</code>。
     * @return 类型为 <code>ApiResponse&lt;CurrentAccountVO&gt;</code> 的处理结果
     */
    @GetMapping("/me")
    public ApiResponse<CurrentAccountVO> me() {
        return ApiResponse.ok(identity.currentView());
    }

    /**
     * 更新Me。
     * HTTP 映射：<code>@PatchMapping("/me")</code>。
     * 主要协作调用：<code>auth.updateMe</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>UpdateMyAccountDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;CurrentAccountVO&gt;</code> 的处理结果
     */
    @PatchMapping("/me")
    public ApiResponse<CurrentAccountVO> updateMe(@Valid @RequestBody UpdateMyAccountDTO request) {
        return ApiResponse.ok(auth.updateMe(request));
    }

    /**
     * 发送邮箱代码。
     * HTTP 映射：<code>@PostMapping("/me/email/verification-codes")</code>。
     * 主要协作调用：<code>verification.sendForCurrentAccount</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;EmailVerificationCodeVO&gt;</code> 的处理结果
     */
    @PostMapping("/me/email/verification-codes")
    public ApiResponse<EmailVerificationCodeVO> sendEmailCode(HttpServletRequest request) {
        // 发信失败的审计由 EmailVerificationService 写到独立事务里（AccountAuditService.failedIndependently），
        // 因此接入层不再需要 try/catch —— 那条规则已经回到服务层自己身上。
        return ApiResponse.ok(verification.sendForCurrentAccount(request.getRemoteAddr()));
    }

    /**
     * 确认邮箱。
     * HTTP 映射：<code>@PostMapping("/me/email/confirm")</code>。
     * 主要协作调用：<code>auth.confirmEmail</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ConfirmEmailDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;CurrentAccountVO&gt;</code> 的处理结果
     */
    @PostMapping("/me/email/confirm")
    public ApiResponse<CurrentAccountVO> confirmEmail(@Valid @RequestBody ConfirmEmailDTO request) {
        return ApiResponse.ok(auth.confirmEmail(request));
    }

    /**
     * change密码处理入口。
     * HTTP 映射：<code>@PutMapping("/me/password")</code>。
     * 主要协作调用：<code>auth.changePassword</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ChangePasswordDTO</code>；嵌套字段继续执行校验
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request,
                                            HttpServletRequest servletRequest) {
        auth.changePassword(request);
        if (servletRequest.getSession(false) != null) {
            servletRequest.getSession(false).invalidate();
        }
        return ApiResponse.ok(null);
    }

    /**
     * 接受邀请。
     * HTTP 映射：<code>@PostMapping("/invitations/accept")</code>。
     * 主要协作调用：<code>admin.acceptInvitation</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>AcceptAdminInvitationDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/invitations/accept")
    public ApiResponse<Void> acceptInvitation(@Valid @RequestBody AcceptAdminInvitationDTO request) {
        admin.acceptInvitation(request);
        return ApiResponse.ok(null);
    }

    /**
     * myInvitations处理入口。
     * HTTP 映射：<code>@GetMapping("/invitations")</code>。
     * 主要协作调用：<code>admin.myInvitations</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;InvitationRecordVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/invitations")
    public ApiResponse<List<InvitationRecordVO>> myInvitations() {
        return ApiResponse.ok(admin.myInvitations());
    }

    /**
     * 接受邀请依据标识。
     * HTTP 映射：<code>@PostMapping("/invitations/{invitationId}/accept")</code>。
     * 主要协作调用：<code>admin.acceptInvitationById</code>。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/invitations/{invitationId}/accept")
    public ApiResponse<Void> acceptInvitationById(@PathVariable String invitationId) {
        admin.acceptInvitationById(invitationId);
        return ApiResponse.ok(null);
    }
}


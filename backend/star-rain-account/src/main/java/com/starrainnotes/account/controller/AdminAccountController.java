/*
 * AdminAccountController：账号模块的HTTP 接入文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：CreateAdminInvitationDTO、AccountStatusDTO、AdminAccountVO、AccountAuditVO、AdminInvitationVO、InvitationRecordVO、AdminAccountService。
 * 阅读入口：accounts（账号集合处理入口）；changeStatus（change状态处理入口）；createInvitation（创建邀请）；invitations（invitations处理入口）。
 */

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

/**
 * 后台账号控制器，属于账号模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    /**
     * 业务服务。
     * 协作类型：<code>AdminAccountService</code>。
     */
    private final AdminAccountService service;

    /**
     * 构造AdminAccountController，接收本类使用的依赖或初始化参数。
     *
     * @param service 业务服务；类型为 <code>AdminAccountService</code>
     */
    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    /**
     * 账号集合处理入口。
     * HTTP 映射：<code>@GetMapping("/accounts")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:read\')")</code>。
     * 主要协作调用：<code>service.accounts</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;AdminAccountVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:read')")
    public ApiResponse<PageResult<AdminAccountVO>> accounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.accounts(page, pageSize, keyword, status));
    }

    /**
     * change状态处理入口。
     * HTTP 映射：<code>@PatchMapping("/accounts/{accountId}/status")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:disable\')")</code>。
     * 主要协作调用：<code>service.changeStatus</code>。
     *
     * @param accountId 账号标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>AccountStatusDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PatchMapping("/accounts/{accountId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:disable')")
    public ApiResponse<Void> changeStatus(@PathVariable String accountId,
                                          @Valid @RequestBody AccountStatusDTO request) {
        service.changeStatus(accountId, request);
        return ApiResponse.ok(null);
    }

    /**
     * 创建邀请。
     * HTTP 映射：<code>@PostMapping("/account-invitations")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:invite-admin\')")</code>。
     * 主要协作调用：<code>service.createInvitation</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CreateAdminInvitationDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;AdminInvitationVO&gt;</code> 的处理结果
     */
    @PostMapping("/account-invitations")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<AdminInvitationVO> createInvitation(
            @Valid @RequestBody CreateAdminInvitationDTO request) {
        return ApiResponse.ok(service.createInvitation(request));
    }

    /**
     * invitations处理入口。
     * HTTP 映射：<code>@GetMapping("/account-invitations")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:invite-admin\')")</code>。
     * 主要协作调用：<code>service.invitations</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;InvitationRecordVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/account-invitations")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<PageResult<InvitationRecordVO>> invitations(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.invitations(page, pageSize));
    }

    /**
     * resend邀请处理入口。
     * HTTP 映射：<code>@PostMapping("/account-invitations/{invitationId}/resend")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:invite-admin\')")</code>。
     * 主要协作调用：<code>service.resendInvitation</code>。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;AdminInvitationVO&gt;</code> 的处理结果
     */
    @PostMapping("/account-invitations/{invitationId}/resend")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<AdminInvitationVO> resendInvitation(@PathVariable String invitationId) {
        return ApiResponse.ok(service.resendInvitation(invitationId));
    }

    /**
     * 撤销邀请。
     * HTTP 映射：<code>@PostMapping("/account-invitations/{invitationId}/revoke")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:invite-admin\')")</code>。
     * 主要协作调用：<code>service.revokeInvitation</code>。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/account-invitations/{invitationId}/revoke")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:invite-admin')")
    public ApiResponse<Void> revokeInvitation(@PathVariable String invitationId) {
        service.revokeInvitation(invitationId);
        return ApiResponse.ok(null);
    }

    /**
     * 移除Administrator。
     * HTTP 映射：<code>@PostMapping("/accounts/{accountId}/revoke-admin")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:revoke-admin\')")</code>。
     * 主要协作调用：<code>service.removeAdministrator</code>。
     *
     * @param accountId 账号标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/accounts/{accountId}/revoke-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('account:revoke-admin')")
    public ApiResponse<Void> removeAdministrator(@PathVariable String accountId) {
        service.removeAdministrator(accountId);
        return ApiResponse.ok(null);
    }

    /**
     * audits处理入口。
     * HTTP 映射：<code>@GetMapping("/account-audits")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'account:audit-read\')")</code>。
     * 主要协作调用：<code>service.audits</code>。
     *
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     * @param targetAccountId 目标账号标识；类型为 <code>Long</code>
     * @param actionCode 动作代码；类型为 <code>String</code>
     * @param result 结果；类型为 <code>String</code>
     * @param startTime 开始时间；类型为 <code>LocalDateTime</code>
     * @param endTime 结束时间；类型为 <code>LocalDateTime</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;AccountAuditVO&gt;&gt;</code> 的处理结果
     */
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

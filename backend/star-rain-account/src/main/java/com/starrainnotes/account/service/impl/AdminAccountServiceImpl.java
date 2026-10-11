/*
 * AdminAccountServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountEntity、RoleEntity、AdminInvitationEntity、AccountAuditEntity、CreateAdminInvitationDTO、AcceptAdminInvitationDTO、AccountStatusDTO。
 * 阅读入口：accounts（账号集合处理入口）；changeStatus（change状态处理入口）；createInvitation（创建邀请）；acceptInvitation（接受邀请）。
 */

package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.entity.RoleEntity;
import com.starrainnotes.account.entity.AdminInvitationEntity;
import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.dto.CreateAdminInvitationDTO;
import com.starrainnotes.account.dto.AcceptAdminInvitationDTO;
import com.starrainnotes.account.dto.AccountStatusDTO;
import com.starrainnotes.account.vo.AdminAccountVO;
import com.starrainnotes.account.vo.AccountAuditVO;
import com.starrainnotes.account.vo.AdminInvitationVO;
import com.starrainnotes.account.vo.InvitationRecordVO;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.service.AccountAuditService;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.account.service.AccountMailService;
import com.starrainnotes.account.service.AdminAccountService;
import com.starrainnotes.account.utils.AccountRules;
import com.starrainnotes.account.utils.OneTimeTokens;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.result.PageResult;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台账号业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;
    /**
     * 身份。
     * 协作类型：<code>AccountIdentityService</code>。
     */
    private final AccountIdentityService identity;
    /**
     * 邮件。
     * 协作类型：<code>AccountMailService</code>。
     */
    private final AccountMailService mail;
    /**
     * 审计。
     * 协作类型：<code>AccountAuditService</code>。
     */
    private final AccountAuditService audit;

    /**
     * 构造AdminAccountServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     * @param identity 身份；类型为 <code>AccountIdentityService</code>
     * @param mail 邮件；类型为 <code>AccountMailService</code>
     * @param audit 审计；类型为 <code>AccountAuditService</code>
     */
    public AdminAccountServiceImpl(AccountMapper mapper, AccountIdentityService identity,
                                   AccountMailService mail, AccountAuditService audit) {
        this.mapper = mapper;
        this.identity = identity;
        this.mail = mail;
        this.audit = audit;
    }

    /**
     * 账号集合处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.accountPageCount</code>、<code>mapper.accountPage</code>、<code>mapper.roleCodes</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;AdminAccountVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminAccountVO> accounts(int page, int pageSize, String keyword, String status) {
        checkPage(page, pageSize);
        if (keyword != null && keyword.length() > 100) {
            throw new ApiException("INVALID_QUERY", "关键词过长", 400);
        }
        if (status != null && !status.isBlank() && !Set.of("ACTIVE", "DISABLED").contains(status)) {
            throw new ApiException("INVALID_QUERY", "账户状态无效", 400);
        }
        long total = mapper.accountPageCount(keyword, status);
        List<AdminAccountVO> items = mapper.accountPage(keyword, status, (page - 1) * pageSize, pageSize)
                .stream().map(account -> new AdminAccountVO(String.valueOf(account.getId()),
                        account.getUsername(), account.getEmail(), account.getDisplayName(), account.getStatus(),
                        Set.copyOf(mapper.roleCodes(account.getId())), account.getCreatedAt())).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    /**
     * change状态处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lockSuperAdminRole</code>、<code>mapper.roleCodes</code>、<code>mapper.activeSuperAdminCount</code>、<code>mapper.updateStatus</code>、<code>mapper.revokePendingInvitations</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>AccountStatusDTO</code>
     */
    @Override
    @Transactional
    public void changeStatus(String targetId, AccountStatusDTO request) {
        long id = AccountRules.id(targetId);
        if (!Set.of("ACTIVE", "DISABLED").contains(request.getStatus())) {
            throw new ApiException("INVALID_STATUS", "账户状态无效", 400);
        }
        mapper.lockSuperAdminRole();
        AccountEntity target = requireAccountForUpdate(id);
        if (target.getStatus().equals(request.getStatus())) {
            return;
        }
        if ("DISABLED".equals(request.getStatus())
                && mapper.roleCodes(id).contains("SUPER_ADMIN")
                && mapper.activeSuperAdminCount() <= 1) {
            throw new ApiException("LAST_SUPER_ADMIN_PROTECTED", "不能停用最后一位超级管理员", 409);
        }
        mapper.updateStatus(id, request.getStatus());
        if ("DISABLED".equals(request.getStatus())) {
            mapper.revokePendingInvitations(id);
        }
        audit.success(identity.principal().getAccountId(), id,
                "ACTIVE".equals(request.getStatus()) ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED");
    }

    /**
     * 创建邀请。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.roleCodes</code>、<code>mapper.pendingInvitationCount</code>、<code>identity.principal</code>、<code>mapper.insertInvitation</code>、<code>audit.success</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CreateAdminInvitationDTO</code>
     * @return 类型为 <code>AdminInvitationVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminInvitationVO createInvitation(CreateAdminInvitationDTO request) {
        long targetId = AccountRules.id(request.getTargetAccountId());
        AccountEntity target = requireAccountForUpdate(targetId);
        if (!"ACTIVE".equals(target.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "目标账户不可用", 409);
        }
        List<String> roles = mapper.roleCodes(targetId);
        if (!roles.contains("USER") || roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")) {
            throw new ApiException("ROLE_ASSIGNMENT_CONFLICT", "目标账户不符合管理员邀请条件", 409);
        }
        if (mapper.pendingInvitationCount(targetId) > 0) {
            throw new ApiException("ADMIN_INVITATION_EXISTS", "目标账户已有待接受邀请", 409);
        }
        String token = OneTimeTokens.create();
        AdminInvitationEntity invitation = new AdminInvitationEntity();
        invitation.setTargetAccountId(targetId);
        invitation.setEmailSnapshot(target.getEmail());
        invitation.setTargetRoleCode("ADMIN");
        invitation.setTokenHash(OneTimeTokens.hash(token));
        invitation.setStatus("PENDING");
        invitation.setInvitedBy(identity.principal().getAccountId());
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(48));
        mapper.insertInvitation(invitation);
        deliverInvitation(invitation, token);
        audit.success(invitation.getInvitedBy(), targetId, "ADMIN_INVITED");
        return new AdminInvitationVO(String.valueOf(invitation.getId()), String.valueOf(targetId),
                invitation.getExpiresAt(), mail.invitationUrl(token), target.getEmail(), "SUBMITTED");
    }

    /**
     * 接受邀请。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>identity.principal</code>、<code>mapper.invitationByHash</code>、<code>mapper.invitationByHashForUpdate</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>AcceptAdminInvitationDTO</code>
     */
    @Override
    @Transactional
    public void acceptInvitation(AcceptAdminInvitationDTO request) {
        long actorId = identity.principal().getAccountId();
        AdminInvitationEntity reference = mapper.invitationByHash(OneTimeTokens.hash(request.getToken()));
        if (reference == null) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请链接无效", 400);
        }
        requireAccountForUpdate(reference.getTargetAccountId());
        AdminInvitationEntity invitation = mapper.invitationByHashForUpdate(OneTimeTokens.hash(request.getToken()));
        accept(invitation, actorId);
    }

    /**
     * 接受邀请依据标识。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>identity.principal</code>、<code>mapper.invitationByIdForUpdate</code>。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void acceptInvitationById(String invitationId) {
        long actorId = identity.principal().getAccountId();
        requireAccountForUpdate(actorId);
        AdminInvitationEntity invitation = mapper.invitationByIdForUpdate(AccountRules.id(invitationId));
        accept(invitation, actorId);
    }

    /**
     * 接受后台账号。
     * 主要协作调用：<code>mapper.roleByCode</code>、<code>mapper.roleCodes</code>、<code>mapper.acceptInvitation</code>、<code>mapper.insertAccountRole</code>、<code>mapper.incrementAuthVersion</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param invitation 邀请；类型为 <code>AdminInvitationEntity</code>
     * @param actorId 操作者标识；类型为 <code>long</code>
     */
    private void accept(AdminInvitationEntity invitation, long actorId) {
        if (invitation == null) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请链接无效", 400);
        }
        if (invitation.getTargetAccountId().longValue() != actorId) {
            throw new ApiException("ADMIN_INVITATION_TARGET_MISMATCH", "此邀请不属于当前账户", 403);
        }
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new ApiException("ADMIN_INVITATION_USED", "邀请已失效", 409);
        }
        if (!invitation.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException("ADMIN_INVITATION_EXPIRED", "邀请已过期", 409);
        }
        AccountEntity target = requireAccountForUpdate(actorId);
        if (!"ACTIVE".equals(target.getStatus()) || !"ADMIN".equals(invitation.getTargetRoleCode())
                || !target.getEmail().equals(invitation.getEmailSnapshot())) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请条件已变化", 409);
        }
        RoleEntity role = mapper.roleByCode("ADMIN");
        if (role == null || !"ENABLED".equals(role.getStatus())) {
            throw new ApiException("ROLE_NOT_FOUND", "管理员角色不可用", 409);
        }
        if (mapper.roleCodes(actorId).contains("ADMIN")) {
            throw new ApiException("ADMIN_INVITATION_USED", "账户已拥有管理员角色", 409);
        }
        if (mapper.acceptInvitation(invitation.getId(), actorId) != 1) {
            throw new ApiException("ADMIN_INVITATION_USED", "邀请已失效", 409);
        }
        mapper.insertAccountRole(actorId, role.getId(), invitation.getInvitedBy());
        mapper.incrementAuthVersion(actorId);
        audit.success(actorId, actorId, "ADMIN_INVITATION_ACCEPTED");
    }

    /**
     * invitations处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.invitationPage</code>、<code>mapper.invitationPageCount</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;InvitationRecordVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<InvitationRecordVO> invitations(int page, int pageSize) {
        checkPage(page, pageSize);
        return new PageResult<>(mapper.invitationPage((page - 1) * pageSize, pageSize).stream()
                .map(this::invitationView).toList(), mapper.invitationPageCount(), page, pageSize);
    }

    /**
     * myInvitations处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.pendingInvitationsForAccount</code>、<code>identity.principal</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<InvitationRecordVO> myInvitations() {
        return mapper.pendingInvitationsForAccount(identity.principal().getAccountId()).stream()
                .map(this::invitationView).toList();
    }

    /**
     * resend邀请处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.roleCodes</code>、<code>mapper.pendingInvitationCount</code>、<code>audit.success</code>、<code>identity.principal</code>、<code>mail.invitationUrl</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>AdminInvitationVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminInvitationVO resendInvitation(String invitationId) {
        AdminInvitationEntity invitation = lockedInvitation(invitationId);
        if (!Set.of("PENDING", "EXPIRED").contains(invitation.getStatus())) {
            throw new ApiException("ADMIN_INVITATION_USED", "此邀请已经处理，不能重发", 409);
        }
        AccountEntity target = requireAccountForUpdate(invitation.getTargetAccountId());
        if (!"ACTIVE".equals(target.getStatus()) || !target.getEmail().equals(invitation.getEmailSnapshot())
                || mapper.roleCodes(target.getId()).contains("ADMIN")
                || mapper.roleCodes(target.getId()).contains("SUPER_ADMIN")) {
            throw new ApiException("ROLE_ASSIGNMENT_CONFLICT", "目标账户不符合管理员邀请条件", 409);
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime previous = invitation.getLastSentAt() == null ? invitation.getCreatedAt() : invitation.getLastSentAt();
        if (previous.plusMinutes(1).isAfter(now)) {
            throw new ApiException("INVITATION_SEND_COOLDOWN", "请等待 60 秒后再重发", 429);
        }
        if (mapper.pendingInvitationCount(target.getId()) > 0 && !"PENDING".equals(invitationView(invitation).getStatus())) {
            throw new ApiException("ADMIN_INVITATION_EXISTS", "该账户已有另一条待处理邀请，请处理最新邀请", 409);
        }
        String token = OneTimeTokens.create();
        invitation.setTokenHash(OneTimeTokens.hash(token));
        invitation.setExpiresAt(now.plusHours(48));
        deliverInvitation(invitation, token);
        audit.success(identity.principal().getAccountId(), target.getId(), "ADMIN_INVITATION_RESENT");
        return new AdminInvitationVO(String.valueOf(invitation.getId()), String.valueOf(target.getId()),
                invitation.getExpiresAt(), mail.invitationUrl(token), target.getEmail(), "SUBMITTED");
    }

    /**
     * 撤销邀请。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.revokeInvitation</code>、<code>audit.success</code>、<code>identity.principal</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void revokeInvitation(String invitationId) {
        AdminInvitationEntity invitation = lockedInvitation(invitationId);
        if (!Set.of("PENDING", "EXPIRED").contains(invitation.getStatus())) {
            throw new ApiException("ADMIN_INVITATION_USED", "此邀请已经处理", 409);
        }
        mapper.revokeInvitation(invitation.getId());
        audit.success(identity.principal().getAccountId(), invitation.getTargetAccountId(), "ADMIN_INVITATION_REVOKED");
    }

    /**
     * 移除Administrator。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lockSuperAdminRole</code>、<code>mapper.roleCodes</code>、<code>mapper.deleteAccountRole</code>、<code>mapper.roleByCode</code>、<code>mapper.revokePendingInvitations</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void removeAdministrator(String targetId) {
        long id = AccountRules.id(targetId);
        mapper.lockSuperAdminRole();
        requireAccountForUpdate(id);
        List<String> roles = mapper.roleCodes(id);
        if (roles.contains("SUPER_ADMIN")) {
            throw new ApiException("SUPER_ADMIN_PROTECTED", "超级管理员身份不能通过此操作取消", 409);
        }
        if (!roles.contains("ADMIN")) {
            throw new ApiException("ROLE_ASSIGNMENT_CONFLICT", "该账户不是管理员", 409);
        }
        mapper.deleteAccountRole(id, mapper.roleByCode("ADMIN").getId());
        mapper.revokePendingInvitations(id);
        mapper.incrementAuthVersion(id);
        audit.success(identity.principal().getAccountId(), id, "ADMIN_ROLE_REVOKED");
    }

    /**
     * locked邀请处理入口。
     * 主要协作调用：<code>mapper.invitationById</code>、<code>mapper.invitationByIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>AdminInvitationEntity</code> 的处理结果
     */
    private AdminInvitationEntity lockedInvitation(String invitationId) {
        long id = AccountRules.id(invitationId);
        AdminInvitationEntity reference = mapper.invitationById(id);
        if (reference == null) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请不存在", 404);
        }
        requireAccountForUpdate(reference.getTargetAccountId());
        return mapper.invitationByIdForUpdate(id);
    }

    /**
     * deliver邀请处理入口。
     * 主要协作调用：<code>mail.sendInvitation</code>、<code>mapper.saveInvitationDelivery</code>。
     *
     * @param invitation 邀请；类型为 <code>AdminInvitationEntity</code>
     * @param token 令牌；类型为 <code>String</code>
     */
    private void deliverInvitation(AdminInvitationEntity invitation, String token) {
        invitation.setMailMessageId(mail.sendInvitation(invitation.getEmailSnapshot(), token));
        invitation.setLastSentAt(LocalDateTime.now(ZoneOffset.UTC));
        mapper.saveInvitationDelivery(invitation);
    }

    /**
     * 邀请View处理入口。
     * 关键处理步骤调用 <code>invitation.getStatus</code>、<code>invitation.getExpiresAt</code>、<code>LocalDateTime.now</code>、<code>String.valueOf</code>。
     *
     * @param invitation 邀请；类型为 <code>AdminInvitationEntity</code>
     * @return 类型为 <code>InvitationRecordVO</code> 的处理结果
     */
    private InvitationRecordVO invitationView(AdminInvitationEntity invitation) {
        String status = invitation.getStatus();
        if ("PENDING".equals(status) && !invitation.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            status = "EXPIRED";
        }
        return new InvitationRecordVO(String.valueOf(invitation.getId()), String.valueOf(invitation.getTargetAccountId()),
                invitation.getEmailSnapshot(), status, invitation.getCreatedAt(), invitation.getExpiresAt(),
                invitation.getLastSentAt(), invitation.getMailSubmissionStatus());
    }

    /**
     * audits处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.auditPageCount</code>、<code>mapper.auditPage</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @param result 结果；类型为 <code>String</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>
     * @param end 结束；类型为 <code>LocalDateTime</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;AccountAuditVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AccountAuditVO> audits(Long actor, Long target, String action, String result,
                                        LocalDateTime start, LocalDateTime end, int page, int pageSize) {
        checkPage(page, pageSize);
        if (action != null && action.length() > 80) {
            throw new ApiException("INVALID_QUERY", "审计动作过长", 400);
        }
        if (result != null && !result.isBlank() && !Set.of("SUCCESS", "FAILED").contains(result)) {
            throw new ApiException("INVALID_QUERY", "审计结果无效", 400);
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new ApiException("INVALID_QUERY", "时间范围无效", 400);
        }
        long total = mapper.auditPageCount(actor, target, action, result, start, end);
        List<AccountAuditVO> items = new ArrayList<>();
        for (AccountAuditEntity row : mapper.auditPage(actor, target, action, result, start, end,
                (page - 1) * pageSize, pageSize)) {
            items.add(new AccountAuditVO(String.valueOf(row.getId()),
                    row.getActorAccountId() == null ? null : String.valueOf(row.getActorAccountId()),
                    row.getTargetAccountId() == null ? null : String.valueOf(row.getTargetAccountId()),
                    row.getActionCode(), row.getResult(), row.getIpAddress(), row.getCreatedAt()));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    /**
     * 检查所需账号面向更新。
     * 主要协作调用：<code>mapper.accountByIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AccountEntity</code> 的处理结果
     */
    private AccountEntity requireAccountForUpdate(long id) {
        AccountEntity account = mapper.accountByIdForUpdate(id);
        if (account == null) {
            throw new ApiException("ACCOUNT_NOT_FOUND", "账户不存在", 404);
        }
        return account;
    }

    /**
     * 检查分页。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     */
    private static void checkPage(int page, int pageSize) {
        if (page < 1 || page > 1_000_000 || pageSize < 1 || pageSize > 100) {
            throw new ApiException("INVALID_PAGE", "分页参数无效", 400);
        }
    }
}

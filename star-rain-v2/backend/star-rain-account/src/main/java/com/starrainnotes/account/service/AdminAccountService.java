package com.starrainnotes.account.service;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.support.AccountRules;
import com.starrainnotes.account.support.OneTimeTokens;
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
import com.starrainnotes.common.ApiException;
import com.starrainnotes.common.PageResult;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    private final AccountMapper mapper;
    private final AccountIdentityService identity;
    private final AccountMailService mail;
    private final AccountAuditService audit;

    public AdminAccountService(AccountMapper mapper, AccountIdentityService identity,
                               AccountMailService mail, AccountAuditService audit) {
        this.mapper = mapper;
        this.identity = identity;
        this.mail = mail;
        this.audit = audit;
    }

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

    @Transactional
    public void changeStatus(String targetId, AccountStatusDTO request) {
        long id = AccountRules.id(targetId);
        if (!Set.of("ACTIVE", "DISABLED").contains(request.status())) {
            throw new ApiException("INVALID_STATUS", "账户状态无效", 400);
        }
        mapper.lockSuperAdminRole();
        AccountEntity target = requireAccountForUpdate(id);
        if (target.getStatus().equals(request.status())) {
            return;
        }
        if ("DISABLED".equals(request.status())
                && mapper.roleCodes(id).contains("SUPER_ADMIN")
                && mapper.activeSuperAdminCount() <= 1) {
            throw new ApiException("LAST_SUPER_ADMIN_PROTECTED", "不能停用最后一位超级管理员", 409);
        }
        mapper.updateStatus(id, request.status());
        if ("DISABLED".equals(request.status())) {
            mapper.revokePendingInvitations(id);
        }
        audit.success(identity.principal().accountId(), id,
                "ACTIVE".equals(request.status()) ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED");
    }

    @Transactional
    public AdminInvitationVO createInvitation(CreateAdminInvitationDTO request) {
        long targetId = AccountRules.id(request.targetAccountId());
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
        invitation.setInvitedBy(identity.principal().accountId());
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(48));
        mapper.insertInvitation(invitation);
        deliverInvitation(invitation, token);
        audit.success(invitation.getInvitedBy(), targetId, "ADMIN_INVITED");
        return new AdminInvitationVO(String.valueOf(invitation.getId()), String.valueOf(targetId),
                invitation.getExpiresAt(), mail.invitationUrl(token), target.getEmail(), "SUBMITTED");
    }

    @Transactional
    public void acceptInvitation(AcceptAdminInvitationDTO request) {
        long actorId = identity.principal().accountId();
        AdminInvitationEntity reference = mapper.invitationByHash(OneTimeTokens.hash(request.token()));
        if (reference == null) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请链接无效", 400);
        }
        requireAccountForUpdate(reference.getTargetAccountId());
        AdminInvitationEntity invitation = mapper.invitationByHashForUpdate(OneTimeTokens.hash(request.token()));
        accept(invitation, actorId);
    }

    @Transactional
    public void acceptInvitationById(String invitationId) {
        long actorId = identity.principal().accountId();
        requireAccountForUpdate(actorId);
        AdminInvitationEntity invitation = mapper.invitationByIdForUpdate(AccountRules.id(invitationId));
        accept(invitation, actorId);
    }

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

    @Transactional(readOnly = true)
    public PageResult<InvitationRecordVO> invitations(int page, int pageSize) {
        checkPage(page, pageSize);
        return new PageResult<>(mapper.invitationPage((page - 1) * pageSize, pageSize).stream()
                .map(this::invitationView).toList(), mapper.invitationPageCount(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public List<InvitationRecordVO> myInvitations() {
        return mapper.pendingInvitationsForAccount(identity.principal().accountId()).stream()
                .map(this::invitationView).toList();
    }

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
        if (mapper.pendingInvitationCount(target.getId()) > 0 && !"PENDING".equals(invitationView(invitation).status())) {
            throw new ApiException("ADMIN_INVITATION_EXISTS", "该账户已有另一条待处理邀请，请处理最新邀请", 409);
        }
        String token = OneTimeTokens.create();
        invitation.setTokenHash(OneTimeTokens.hash(token));
        invitation.setExpiresAt(now.plusHours(48));
        deliverInvitation(invitation, token);
        audit.success(identity.principal().accountId(), target.getId(), "ADMIN_INVITATION_RESENT");
        return new AdminInvitationVO(String.valueOf(invitation.getId()), String.valueOf(target.getId()),
                invitation.getExpiresAt(), mail.invitationUrl(token), target.getEmail(), "SUBMITTED");
    }

    @Transactional
    public void revokeInvitation(String invitationId) {
        AdminInvitationEntity invitation = lockedInvitation(invitationId);
        if (!Set.of("PENDING", "EXPIRED").contains(invitation.getStatus())) {
            throw new ApiException("ADMIN_INVITATION_USED", "此邀请已经处理", 409);
        }
        mapper.revokeInvitation(invitation.getId());
        audit.success(identity.principal().accountId(), invitation.getTargetAccountId(), "ADMIN_INVITATION_REVOKED");
    }

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
        audit.success(identity.principal().accountId(), id, "ADMIN_ROLE_REVOKED");
    }

    private AdminInvitationEntity lockedInvitation(String invitationId) {
        long id = AccountRules.id(invitationId);
        AdminInvitationEntity reference = mapper.invitationById(id);
        if (reference == null) {
            throw new ApiException("INVALID_ADMIN_INVITATION", "邀请不存在", 404);
        }
        requireAccountForUpdate(reference.getTargetAccountId());
        return mapper.invitationByIdForUpdate(id);
    }

    private void deliverInvitation(AdminInvitationEntity invitation, String token) {
        invitation.setMailMessageId(mail.sendInvitation(invitation.getEmailSnapshot(), token));
        invitation.setLastSentAt(LocalDateTime.now(ZoneOffset.UTC));
        mapper.saveInvitationDelivery(invitation);
    }

    private InvitationRecordVO invitationView(AdminInvitationEntity invitation) {
        String status = invitation.getStatus();
        if ("PENDING".equals(status) && !invitation.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            status = "EXPIRED";
        }
        return new InvitationRecordVO(String.valueOf(invitation.getId()), String.valueOf(invitation.getTargetAccountId()),
                invitation.getEmailSnapshot(), status, invitation.getCreatedAt(), invitation.getExpiresAt(),
                invitation.getLastSentAt(), invitation.getMailSubmissionStatus());
    }

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

    private AccountEntity requireAccountForUpdate(long id) {
        AccountEntity account = mapper.accountByIdForUpdate(id);
        if (account == null) {
            throw new ApiException("ACCOUNT_NOT_FOUND", "账户不存在", 404);
        }
        return account;
    }

    private static void checkPage(int page, int pageSize) {
        if (page < 1 || page > 1_000_000 || pageSize < 1 || pageSize > 100) {
            throw new ApiException("INVALID_PAGE", "分页参数无效", 400);
        }
    }
}

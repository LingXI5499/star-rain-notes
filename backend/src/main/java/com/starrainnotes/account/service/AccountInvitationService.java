package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.config.AccountProperties;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.entity.AdminInvitation;
import com.starrainnotes.account.mapper.AccountUserMapper;
import com.starrainnotes.account.mapper.AdminInvitationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountInvitationService {
    private final AccountUserMapper userMapper;
    private final AdminInvitationMapper invitationMapper;
    private final VerificationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final MailGateway mailGateway;
    private final AccountProperties properties;
    private final AuditLogService auditLog;

    public AccountInvitationService(AccountUserMapper userMapper, AdminInvitationMapper invitationMapper,
                                    VerificationCodeService codeService, PasswordEncoder passwordEncoder,
                                    MailGateway mailGateway, AccountProperties properties, AuditLogService auditLog) {
        this.userMapper = userMapper;
        this.invitationMapper = invitationMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.mailGateway = mailGateway;
        this.properties = properties;
        this.auditLog = auditLog;
    }

    @Transactional
    public IssuedInvitation create(String email, Long invitedByAccountId) {
        requireSuperAdmin(invitedByAccountId, "Only a super administrator can invite.");
        String normalized = AccountService.normalize(email);
        if (!AccountService.validEmail(normalized)) {
            throw AccountService.fail("INVALID_EMAIL", HttpStatus.UNPROCESSABLE_ENTITY, "Invalid email", "Invalid email address.");
        }
        if (findByEmail(normalized) != null) {
            throw AccountService.fail("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT, "Already registered",
                    "This email already has an account.");
        }
        if (hasPendingInvitation(normalized)) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Already invited",
                    "This email already has a pending invitation.");
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        AdminInvitation invitation = new AdminInvitation();
        invitation.setEmail(normalized);
        invitation.setTokenHash(VerificationCodeService.sha256(rawToken));
        invitation.setStatus("PENDING");
        invitation.setInvitedBy(invitedByAccountId);
        invitation.setExpiresAt(now().plusHours(72));
        invitation.setSentAt(now());
        invitationMapper.insert(invitation);
        String link = invitationLink(rawToken);
        mailGateway.sendInvitationLink(invitation.getEmail(), link);
        auditLog.record(invitedByAccountId, "INVITATION_CREATED", "INVITATION", invitation.getId(), "SUCCESS",
                null, null, Map.of("email", AccountService.mask(invitation.getEmail())));
        return new IssuedInvitation(invitation, link);
    }

    public AdminInvitation byToken(String rawToken) {
        AdminInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenHash, VerificationCodeService.sha256(rawToken)).last("LIMIT 1"));
        if (invitation == null) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.NOT_FOUND, "Invalid invitation", "The invitation does not exist.");
        }
        if ("REVOKED".equals(invitation.getStatus())) {
            throw AccountService.fail("INVITATION_REVOKED", HttpStatus.GONE, "Invitation revoked", "The invitation was revoked.");
        }
        if ("ACCEPTED".equals(invitation.getStatus())) {
            throw AccountService.fail("INVITATION_EXPIRED", HttpStatus.GONE, "Already used", "The invitation was already used.");
        }
        if (invitation.getExpiresAt().isBefore(now())) {
            invitation.setStatus("EXPIRED");
            invitationMapper.updateById(invitation);
            throw AccountService.fail("INVITATION_EXPIRED", HttpStatus.GONE, "Invitation expired", "The invitation has expired.");
        }
        return invitation;
    }

    public void requestCode(String rawToken, String ip) {
        AdminInvitation invitation = byToken(rawToken);
        codeService.issue(invitation.getEmail(), "ADMIN_REGISTRATION", invitation.getId(), ip);
    }

    @Transactional
    public AccountUser register(String rawToken, String email, String code, String password) {
        AdminInvitation invitation = byToken(rawToken);
        String normalizedEmail = AccountService.normalize(email);
        if (normalizedEmail != null && !normalizedEmail.isBlank() && !normalizedEmail.equals(invitation.getEmail())) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "Email mismatch",
                    "The email must match the invitation.");
        }
        codeService.verify(invitation.getEmail(), "ADMIN_REGISTRATION", code, invitation.getId());
        AccountUser account = new AccountUser();
        account.setEmail(invitation.getEmail());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(AccountService.ADMIN);
        account.setAccountStatus(AccountService.ACTIVE);
        account.setEmailVerifiedAt(now());
        account.setActivatedAt(now());
        userMapper.insert(account);
        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedAccountId(account.getId());
        invitation.setAcceptedAt(now());
        invitationMapper.updateById(invitation);
        auditLog.record(account.getId(), "ADMIN_REGISTERED", "ACCOUNT", account.getId(), "SUCCESS",
                null, null, Map.of("invitationId", invitation.getId()));
        return account;
    }

    @Transactional
    public IssuedInvitation resend(Long invitationId) {
        AdminInvitation invitation = requireInvitation(invitationId);
        if (!"PENDING".equals(invitation.getStatus())) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Invitation not pending",
                    "Only a pending invitation can be resent.");
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        invitation.setTokenHash(VerificationCodeService.sha256(rawToken));
        invitation.setSentAt(now());
        invitation.setExpiresAt(now().plusHours(72));
        invitationMapper.updateById(invitation);
        String link = invitationLink(rawToken);
        mailGateway.sendInvitationLink(invitation.getEmail(), link);
        auditLog.record(invitation.getInvitedBy(), "INVITATION_RESENT", "INVITATION", invitation.getId(), "SUCCESS",
                null, null, Map.of("email", AccountService.mask(invitation.getEmail())));
        return new IssuedInvitation(invitation, link);
    }

    @Transactional
    public void revoke(Long invitationId, Long operatorId) {
        AdminInvitation invitation = requireInvitation(invitationId);
        if ("ACCEPTED".equals(invitation.getStatus())) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Invitation already accepted",
                    "An accepted invitation cannot be revoked.");
        }
        invitation.setStatus("REVOKED");
        invitation.setRevokedAt(now());
        invitationMapper.updateById(invitation);
        auditLog.record(operatorId, "INVITATION_REVOKED", "INVITATION", invitation.getId(), "SUCCESS",
                null, null, Map.of("email", AccountService.mask(invitation.getEmail())));
    }

    @Transactional
    public void delete(Long invitationId, Long operatorId) {
        AdminInvitation invitation = requireInvitation(invitationId);
        boolean expired = invitation.getExpiresAt().isBefore(now());
        if (!"REVOKED".equals(invitation.getStatus()) && !"EXPIRED".equals(invitation.getStatus()) && !expired) {
            throw AccountService.fail("INVITATION_DELETE_FORBIDDEN", HttpStatus.CONFLICT,
                    "Invitation cannot be deleted", "Only revoked or expired invitations can be deleted.");
        }
        String email = AccountService.mask(invitation.getEmail());
        invitationMapper.deleteById(invitationId);
        auditLog.record(operatorId, "INVITATION_DELETED", "INVITATION", invitationId, "SUCCESS",
                null, null, Map.of("email", email));
    }

    @Transactional
    public List<AdminInvitation> list() {
        List<AdminInvitation> invitations = invitationMapper.selectList(new LambdaQueryWrapper<AdminInvitation>()
                .orderByDesc(AdminInvitation::getCreatedAt));
        LocalDateTime current = now();
        invitations.stream()
                .filter(invitation -> "PENDING".equals(invitation.getStatus()) && invitation.getExpiresAt().isBefore(current))
                .forEach(invitation -> {
                    invitation.setStatus("EXPIRED");
                    invitationMapper.updateById(invitation);
                });
        return invitations;
    }

    private void requireSuperAdmin(Long accountId, String detail) {
        AccountUser user = userMapper.selectById(accountId);
        if (user == null) {
            throw AccountService.fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        }
        if (!AccountService.SUPER_ADMIN.equals(user.getRole())) {
            throw AccountService.fail("FORBIDDEN_MODULE", HttpStatus.FORBIDDEN, "Forbidden", detail);
        }
    }

    private AdminInvitation requireInvitation(Long id) {
        AdminInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw AccountService.fail("INVITATION_INVALID", HttpStatus.NOT_FOUND, "Invalid invitation",
                    "The invitation does not exist.");
        }
        return invitation;
    }

    private AccountUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getEmail, email)
                .last("LIMIT 1"));
    }

    private boolean hasPendingInvitation(String email) {
        Long count = invitationMapper.selectCount(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getEmail, email)
                .eq(AdminInvitation::getStatus, "PENDING")
                .gt(AdminInvitation::getExpiresAt, now()));
        return count != null && count > 0;
    }

    private String invitationLink(String rawToken) {
        String base = properties.getMail().getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:5173";
        }
        return base.replaceAll("/+$", "") + "/admin/invitations/" + rawToken;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    public record IssuedInvitation(AdminInvitation invitation, String inviteLink) {
    }
}

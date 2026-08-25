package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.config.AccountProperties;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.entity.AdminInvitation;
import com.starrainnotes.account.mapper.AccountUserMapper;
import com.starrainnotes.account.mapper.AdminInvitationMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String PENDING = "PENDING_ACTIVATION";
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";
    static final int LOCK_AFTER_FAILURES = 5;
    static final long LOCK_MINUTES = 15;

    private final AccountUserMapper userMapper;
    private final AdminInvitationMapper invitationMapper;
    private final VerificationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final MailGateway mailGateway;
    private final AccountProperties props;

    public AccountService(AccountUserMapper userMapper, AdminInvitationMapper invitationMapper,
                          VerificationCodeService codeService, PasswordEncoder passwordEncoder,
                          MailGateway mailGateway, AccountProperties props) {
        this.userMapper = userMapper;
        this.invitationMapper = invitationMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.mailGateway = mailGateway;
        this.props = props;
    }

    public boolean superAdminActivated() {
        Long c = userMapper.selectCount(new LambdaQueryWrapper<AccountUser>()
                .eq(AccountUser::getRole, SUPER_ADMIN).eq(AccountUser::getAccountStatus, ACTIVE));
        return c != null && c > 0;
    }

    public String maskedSuperAdminEmail() { return mask(props.getSuperAdminEmail()); }
    public String configuredSuperAdminEmail() { return props.getSuperAdminEmail(); }

    public void requestActivationCode(String ip) {
        if (superAdminActivated()) {
            throw fail("SUPER_ADMIN_ALREADY_ACTIVATED", HttpStatus.GONE, "Already activated",
                    "A super administrator is already active.");
        }
        String email = props.getSuperAdminEmail();
        if (!validEmail(email)) {
            throw fail("SUPER_ADMIN_EMAIL_NOT_CONFIGURED", HttpStatus.PRECONDITION_FAILED,
                    "Email not configured", "APP_SUPER_ADMIN_EMAIL is missing or invalid.");
        }
        ensurePendingSuperAdmin(email);
        codeService.issue(email, "SUPER_ADMIN_ACTIVATION", null, ip);
    }

    @Transactional
    public AccountUser confirmActivation(String code, String password) {
        if (superAdminActivated()) {
            throw fail("SUPER_ADMIN_ALREADY_ACTIVATED", HttpStatus.GONE, "Already activated",
                    "A super administrator is already active.");
        }
        String email = props.getSuperAdminEmail();
        if (!validEmail(email)) {
            throw fail("SUPER_ADMIN_EMAIL_NOT_CONFIGURED", HttpStatus.PRECONDITION_FAILED,
                    "Email not configured", "APP_SUPER_ADMIN_EMAIL is missing.");
        }
        codeService.verify(email, "SUPER_ADMIN_ACTIVATION", code, null);
        AccountUser account = requireByEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setAccountStatus(ACTIVE);
        account.setEmailVerifiedAt(now());
        account.setActivatedAt(now());
        account.setAuthVersion(account.getAuthVersion() + 1);
        userMapper.updateById(account);
        return account;
    }

    public void createInvitation(String email, Long invitedByAccountId) {
        requireSuperAdmin(invitedByAccountId, "Only a super administrator can invite.");
        String normalized = normalize(email);
        if (!validEmail(normalized)) {
            throw fail("INVALID_EMAIL", HttpStatus.UNPROCESSABLE_ENTITY, "Invalid email", "Invalid email address.");
        }
        if (existsByEmail(normalized)) {
            throw fail("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT, "Already registered",
                    "This email already has an account.");
        }
        if (hasPendingInvitation(normalized)) {
            throw fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Already invited",
                    "This email already has a pending invitation.");
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        AdminInvitation inv = new AdminInvitation();
        inv.setEmail(normalized);
        inv.setTokenHash(VerificationCodeService.sha256(rawToken));
        inv.setStatus("PENDING");
        inv.setInvitedBy(invitedByAccountId);
        inv.setExpiresAt(now().plusHours(72));
        invitationMapper.insert(inv);
        String link = props.getMail().getBaseUrl() + "/admin/invitations/" + rawToken;
        mailGateway.sendInvitationLink(inv.getEmail(), link);
    }

    public AdminInvitation invitationByToken(String rawToken) {
        AdminInvitation inv = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenHash, VerificationCodeService.sha256(rawToken)).last("LIMIT 1"));
        if (inv == null) {
            throw fail("INVITATION_INVALID", HttpStatus.NOT_FOUND, "Invalid invitation", "The invitation does not exist.");
        }
        if ("REVOKED".equals(inv.getStatus())) {
            throw fail("INVITATION_REVOKED", HttpStatus.GONE, "Invitation revoked", "The invitation was revoked.");
        }
        if ("ACCEPTED".equals(inv.getStatus())) {
            throw fail("INVITATION_EXPIRED", HttpStatus.GONE, "Already used", "The invitation was already used.");
        }
        if (inv.getExpiresAt().isBefore(now())) {
            throw fail("INVITATION_EXPIRED", HttpStatus.GONE, "Invitation expired", "The invitation has expired.");
        }
        return inv;
    }

    public void requestInvitationCode(String rawToken, String ip) {
        AdminInvitation inv = invitationByToken(rawToken);
        codeService.issue(inv.getEmail(), "ADMIN_REGISTRATION", inv.getId(), ip);
    }

    @Transactional
    public AccountUser register(String rawToken, String email, String code, String password) {
        AdminInvitation inv = invitationByToken(rawToken);
        if (!normalize(email).equals(inv.getEmail())) {
            throw fail("INVITATION_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "Email mismatch",
                    "The email must match the invitation.");
        }
        codeService.verify(inv.getEmail(), "ADMIN_REGISTRATION", code, inv.getId());
        AccountUser account = new AccountUser();
        account.setEmail(inv.getEmail());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(ADMIN);
        account.setAccountStatus(ACTIVE);
        account.setEmailVerifiedAt(now());
        account.setActivatedAt(now());
        userMapper.insert(account);
        inv.setStatus("ACCEPTED");
        inv.setAcceptedAccountId(account.getId());
        inv.setAcceptedAt(now());
        invitationMapper.updateById(inv);
        return account;
    }

    @Transactional
    public void disable(Long targetId, String reason, Long operatorId) {
        AccountUser target = requireById(targetId);
        if (SUPER_ADMIN.equals(target.getRole())) {
            throw fail("SUPER_ADMIN_PROTECTED", HttpStatus.FORBIDDEN, "Protected",
                    "The super administrator cannot be disabled.");
        }
        target.setAccountStatus(DISABLED);
        target.setDisabledAt(now());
        target.setDisabledBy(operatorId);
        target.setDisabledReason(reason);
        target.setAuthVersion(target.getAuthVersion() + 1);
        userMapper.updateById(target);
    }

    @Transactional
    public void enable(Long targetId) {
        AccountUser target = requireById(targetId);
        target.setAccountStatus(ACTIVE);
        target.setDisabledAt(null);
        target.setAuthVersion(target.getAuthVersion() + 1);
        userMapper.updateById(target);
    }

    public AccountUser authenticate(String email, String password) {
        AccountUser user = findByEmail(normalize(email));
        if (user == null) {
            throw fail("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials",
                    "Invalid email or password.");
        }
        if (DISABLED.equals(user.getAccountStatus())) {
            throw fail("ACCOUNT_DISABLED", HttpStatus.FORBIDDEN, "Account disabled", "This account is disabled.");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now())) {
            throw fail("ACCOUNT_TEMPORARILY_LOCKED", HttpStatus.LOCKED, "Account locked",
                    "Too many failed attempts; try again later.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            int failed = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
            user.setFailedLoginCount(failed);
            if (failed >= LOCK_AFTER_FAILURES) {
                user.setLockedUntil(now().plusMinutes(LOCK_MINUTES));
                user.setFailedLoginCount(0);
            }
            userMapper.updateById(user);
            throw fail("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials",
                    "Invalid email or password.");
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now());
        userMapper.updateById(user);
        return user;
    }

    public List<String> capabilities(String role) {
        if (SUPER_ADMIN.equals(role)) {
            return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE",
                    "SUPER_ADMIN", "REVIEW");
        }
        return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE");
    }

    private void ensurePendingSuperAdmin(String email) {
        AccountUser existing = findByEmail(email);
        if (existing == null) {
            AccountUser account = new AccountUser();
            account.setEmail(email);
            account.setRole(SUPER_ADMIN);
            account.setAccountStatus(PENDING);
            account.setAuthVersion(1);
            userMapper.insert(account);
        }
    }

    private void requireSuperAdmin(Long accountId, String detail) {
        AccountUser user = requireById(accountId);
        if (!SUPER_ADMIN.equals(user.getRole())) {
            throw fail("FORBIDDEN_MODULE", HttpStatus.FORBIDDEN, "Forbidden", detail);
        }
    }

    private AccountUser requireById(Long id) {
        AccountUser user = userMapper.selectById(id);
        if (user == null) throw fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        return user;
    }
    private AccountUser requireByEmail(String email) {
        AccountUser user = findByEmail(email);
        if (user == null) throw fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        return user;
    }
    private AccountUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getEmail, email)
                .last("LIMIT 1"));
    }
    private boolean existsByEmail(String email) { return findByEmail(email) != null; }
    private boolean hasPendingInvitation(String email) {
        Long c = invitationMapper.selectCount(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getEmail, email).eq(AdminInvitation::getStatus, "PENDING"));
        return c != null && c > 0;
    }
    public static String normalize(String email) { return email == null ? null : email.trim().toLowerCase(); }
    public static boolean validEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    public static String mask(String email) {
        if (email == null || email.isBlank()) return "";
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.substring(0, 2) + "***" + email.substring(at);
    }
    private LocalDateTime now() { return LocalDateTime.now(Clock.systemUTC()); }
    static ApiException fail(String code, HttpStatus status, String title, String detail) {
        return new ApiException(status, code, title, detail);
    }
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        AccountUser user = findByEmail(normalize(email));
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw fail("INVALID_CURRENT_PASSWORD", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Current password incorrect", "The current password does not match.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now());
        user.setAuthVersion(user.getAuthVersion() + 1);
        userMapper.updateById(user);
    }

    @Transactional
    public void requestPasswordReset(String email, String ip) {
        String normalized = normalize(email);
        // anti-enumeration: identical response whether or not the email exists
        if (normValidEmail(normalized) && findByEmail(normalized) != null) {
            codeService.issue(normalized, "PASSWORD_RESET", null, ip);
        }
    }

    @Transactional
    public void confirmPasswordReset(String email, String code, String newPassword) {
        String normalized = normalize(email);
        codeService.verify(normalized, "PASSWORD_RESET", code, null);
        AccountUser user = requireByEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now());
        user.setAuthVersion(user.getAuthVersion() + 1);
        userMapper.updateById(user);
    }

    @Transactional
    public void resendInvitation(Long invitationId) {
        AdminInvitation inv = requireInvitation(invitationId);
        if (!"PENDING".equals(inv.getStatus())) {
            throw fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Invitation not pending",
                    "Only a pending invitation can be resent.");
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        inv.setTokenHash(VerificationCodeService.sha256(rawToken));
        inv.setSentAt(now());
        invitationMapper.updateById(inv);
        mailGateway.sendInvitationLink(inv.getEmail(),
                props.getMail().getBaseUrl() + "/admin/invitations/" + rawToken);
    }

    @Transactional
    public void revokeInvitation(Long invitationId) {
        AdminInvitation inv = requireInvitation(invitationId);
        if ("ACCEPTED".equals(inv.getStatus())) {
            throw fail("INVITATION_INVALID", HttpStatus.CONFLICT, "Invitation already accepted",
                    "An accepted invitation cannot be revoked.");
        }
        inv.setStatus("REVOKED");
        inv.setRevokedAt(now());
        invitationMapper.updateById(inv);
    }

    public java.util.List<AdminInvitation> listInvitations() {
        return invitationMapper.selectList(new LambdaQueryWrapper<AdminInvitation>()
                .orderByDesc(AdminInvitation::getCreatedAt));
    }

    private AdminInvitation requireInvitation(Long id) {
        AdminInvitation inv = invitationMapper.selectById(id);
        if (inv == null) throw fail("INVITATION_INVALID", HttpStatus.NOT_FOUND, "Invalid invitation",
                "The invitation does not exist.");
        return inv;
    }

    private boolean normValidEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    public void requireSuperAdminOr401(Long actorId) {
        if (actorId == null) {
            throw fail("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Not authenticated",
                    "Super-admin authentication is required.");
        }
        requireSuperAdmin(actorId, "Super-admin authentication is required.");
    }
    public java.util.List<AccountUser> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<AccountUser>()
                .orderByDesc(AccountUser::getId));
    }
}
package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.config.AccountProperties;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final VerificationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final AccountProperties props;
    private final AuditLogService auditLog;
    private final AccountQueryService queries;

    public AccountService(AccountUserMapper userMapper, VerificationCodeService codeService,
                          PasswordEncoder passwordEncoder, AccountProperties props, AuditLogService auditLog,
                          AccountQueryService queries) {
        this.userMapper = userMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.auditLog = auditLog;
        this.queries = queries;
    }

    public String maskedSuperAdminEmail() { return mask(props.getSuperAdminEmail()); }

    public void requestActivationCode(String ip) {
        if (queries.superAdminActivated()) {
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
        auditLog.record(null, "SUPER_ADMIN_ACTIVATION_CODE", "ACCOUNT", null, "SUCCESS",
                ip, null, Map.of("email", mask(email)));
    }

    @Transactional
    public AccountUser confirmActivation(String code, String password) {
        if (queries.superAdminActivated()) {
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
        auditLog.record(account.getId(), "SUPER_ADMIN_ACTIVATED", "ACCOUNT", account.getId(), "SUCCESS",
                null, null, null);
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
        auditLog.record(operatorId, "ACCOUNT_DISABLED", "ACCOUNT", target.getId(), "SUCCESS",
                null, null, Map.of("email", mask(target.getEmail())));
    }

    @Transactional
    public void enable(Long targetId, Long operatorId) {
        AccountUser target = requireById(targetId);
        target.setAccountStatus(ACTIVE);
        target.setDisabledAt(null);
        target.setDisabledBy(null);
        target.setDisabledReason(null);
        target.setAuthVersion(target.getAuthVersion() + 1);
        userMapper.updateById(target);
        auditLog.record(operatorId, "ACCOUNT_ENABLED", "ACCOUNT", target.getId(), "SUCCESS",
                null, null, Map.of("email", mask(target.getEmail())));
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
            boolean locked = false;
            if (failed >= LOCK_AFTER_FAILURES) {
                user.setLockedUntil(now().plusMinutes(LOCK_MINUTES));
                user.setFailedLoginCount(0);
                locked = true;
            }
            userMapper.updateById(user);
            if (locked) {
                auditLog.record(user.getId(), "ACCOUNT_LOCKED", "ACCOUNT", user.getId(), "FAILURE",
                        null, null, null);
            }
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
    public static ApiException fail(String code, HttpStatus status, String title, String detail) {
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
    public AccountUser findByEmailPublic(String email) {
        return findByEmail(normalize(email));
    }
}

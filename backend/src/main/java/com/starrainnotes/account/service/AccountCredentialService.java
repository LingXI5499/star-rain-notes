package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AccountCredentialService {
    private static final int LOCK_AFTER_FAILURES = 5;
    private static final long LOCK_MINUTES = 15;

    private final AccountUserMapper userMapper;
    private final VerificationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;

    public AccountCredentialService(AccountUserMapper userMapper, VerificationCodeService codeService,
                                    PasswordEncoder passwordEncoder, AuditLogService auditLog) {
        this.userMapper = userMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
    }

    public AccountUser authenticate(String email, String password) {
        AccountUser user = findByEmail(AccountService.normalize(email));
        if (user == null) {
            throw AccountService.fail("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials",
                    "Invalid email or password.");
        }
        if (AccountService.DISABLED.equals(user.getAccountStatus())) {
            throw AccountService.fail("ACCOUNT_DISABLED", HttpStatus.FORBIDDEN, "Account disabled", "This account is disabled.");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now())) {
            throw AccountService.fail("ACCOUNT_TEMPORARILY_LOCKED", HttpStatus.LOCKED, "Account locked",
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
            throw AccountService.fail("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials",
                    "Invalid email or password.");
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        AccountUser user = findByEmail(AccountService.normalize(email));
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw AccountService.fail("INVALID_CURRENT_PASSWORD", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Current password incorrect", "The current password does not match.");
        }
        updatePassword(user, newPassword);
    }

    @Transactional
    public void requestPasswordReset(String email, String ip) {
        String normalized = AccountService.normalize(email);
        // anti-enumeration: identical response whether or not the email exists
        if (AccountService.validEmail(normalized) && findByEmail(normalized) != null) {
            codeService.issue(normalized, "PASSWORD_RESET", null, ip);
        }
    }

    @Transactional
    public void confirmPasswordReset(String email, String code, String newPassword) {
        String normalized = AccountService.normalize(email);
        codeService.verify(normalized, "PASSWORD_RESET", code, null);
        AccountUser user = findByEmail(normalized);
        if (user == null) {
            throw AccountService.fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        }
        updatePassword(user, newPassword);
    }

    private void updatePassword(AccountUser user, String password) {
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangedAt(now());
        user.setAuthVersion(user.getAuthVersion() + 1);
        userMapper.updateById(user);
    }

    private AccountUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getEmail, email)
                .last("LIMIT 1"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}

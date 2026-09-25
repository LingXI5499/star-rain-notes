package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.config.AccountProperties;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AccountActivationService {
    private final AccountUserMapper userMapper;
    private final VerificationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final AccountProperties properties;
    private final AuditLogService auditLog;
    private final AccountQueryService queries;

    public AccountActivationService(AccountUserMapper userMapper, VerificationCodeService codeService,
                                    PasswordEncoder passwordEncoder, AccountProperties properties,
                                    AuditLogService auditLog, AccountQueryService queries) {
        this.userMapper = userMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.auditLog = auditLog;
        this.queries = queries;
    }

    @Transactional
    public void requestCode(String ip) {
        if (queries.superAdminActivated()) {
            throw AccountService.fail("SUPER_ADMIN_ALREADY_ACTIVATED", HttpStatus.GONE, "Already activated",
                    "A super administrator is already active.");
        }
        String email = properties.getSuperAdminEmail();
        if (!AccountService.validEmail(email)) {
            throw AccountService.fail("SUPER_ADMIN_EMAIL_NOT_CONFIGURED", HttpStatus.PRECONDITION_FAILED,
                    "Email not configured", "APP_SUPER_ADMIN_EMAIL is missing or invalid.");
        }
        ensurePendingSuperAdmin(email);
        codeService.issue(email, "SUPER_ADMIN_ACTIVATION", null, ip);
        auditLog.record(null, "SUPER_ADMIN_ACTIVATION_CODE", "ACCOUNT", null, "SUCCESS",
                ip, null, Map.of("email", AccountService.mask(email)));
    }

    @Transactional
    public AccountUser confirm(String code, String password) {
        if (queries.superAdminActivated()) {
            throw AccountService.fail("SUPER_ADMIN_ALREADY_ACTIVATED", HttpStatus.GONE, "Already activated",
                    "A super administrator is already active.");
        }
        String email = properties.getSuperAdminEmail();
        if (!AccountService.validEmail(email)) {
            throw AccountService.fail("SUPER_ADMIN_EMAIL_NOT_CONFIGURED", HttpStatus.PRECONDITION_FAILED,
                    "Email not configured", "APP_SUPER_ADMIN_EMAIL is missing.");
        }
        codeService.verify(email, "SUPER_ADMIN_ACTIVATION", code, null);
        AccountUser account = requireByEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setAccountStatus(AccountService.ACTIVE);
        account.setEmailVerifiedAt(now());
        account.setActivatedAt(now());
        account.setAuthVersion(account.getAuthVersion() + 1);
        userMapper.updateById(account);
        auditLog.record(account.getId(), "SUPER_ADMIN_ACTIVATED", "ACCOUNT", account.getId(), "SUCCESS",
                null, null, null);
        return account;
    }

    private void ensurePendingSuperAdmin(String email) {
        if (findByEmail(email) == null) {
            AccountUser account = new AccountUser();
            account.setEmail(email);
            account.setRole(AccountService.SUPER_ADMIN);
            account.setAccountStatus(AccountService.PENDING);
            account.setAuthVersion(1);
            userMapper.insert(account);
        }
    }

    private AccountUser requireByEmail(String email) {
        AccountUser account = findByEmail(email);
        if (account == null) {
            throw AccountService.fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        }
        return account;
    }

    private AccountUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getEmail, email)
                .last("LIMIT 1"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}

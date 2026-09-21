package com.starrainnotes.account.service;

import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AccountAdministrationService {
    private final AccountUserMapper userMapper;
    private final AuditLogService auditLog;

    public AccountAdministrationService(AccountUserMapper userMapper, AuditLogService auditLog) {
        this.userMapper = userMapper;
        this.auditLog = auditLog;
    }

    @Transactional
    public void disable(Long targetId, String reason, Long operatorId) {
        AccountUser target = requireById(targetId);
        if (AccountService.SUPER_ADMIN.equals(target.getRole())) {
            throw AccountService.fail("SUPER_ADMIN_PROTECTED", HttpStatus.FORBIDDEN, "Protected",
                    "The super administrator cannot be disabled.");
        }
        target.setAccountStatus(AccountService.DISABLED);
        target.setDisabledAt(now());
        target.setDisabledBy(operatorId);
        target.setDisabledReason(reason);
        target.setAuthVersion(target.getAuthVersion() + 1);
        userMapper.updateById(target);
        auditLog.record(operatorId, "ACCOUNT_DISABLED", "ACCOUNT", target.getId(), "SUCCESS",
                null, null, Map.of("email", AccountService.mask(target.getEmail())));
    }

    @Transactional
    public void enable(Long targetId, Long operatorId) {
        AccountUser target = requireById(targetId);
        target.setAccountStatus(AccountService.ACTIVE);
        target.setDisabledAt(null);
        target.setDisabledBy(null);
        target.setDisabledReason(null);
        target.setAuthVersion(target.getAuthVersion() + 1);
        userMapper.updateById(target);
        auditLog.record(operatorId, "ACCOUNT_ENABLED", "ACCOUNT", target.getId(), "SUCCESS",
                null, null, Map.of("email", AccountService.mask(target.getEmail())));
    }

    private AccountUser requireById(Long id) {
        AccountUser account = userMapper.selectById(id);
        if (account == null) {
            throw AccountService.fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        }
        return account;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}

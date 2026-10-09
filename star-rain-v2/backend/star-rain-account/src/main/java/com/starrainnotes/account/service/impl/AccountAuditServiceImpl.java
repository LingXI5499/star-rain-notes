package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.service.AccountAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AccountAuditServiceImpl implements AccountAuditService {

    private final AccountMapper mapper;

    public AccountAuditServiceImpl(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void success(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "SUCCESS");
    }

    @Override
    public void failed(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "FAILED");
    }

    /*
     * REQUIRES_NEW：挂起调用方事务，另起一个事务写完就提交。
     * 用于「审计描述的是调用方这次失败」的场景——否则它会跟着回滚一起消失。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failedIndependently(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "FAILED");
    }

    private void writeAudit(Long actor, Long target, String action, String result) {
        AccountAuditEntity row = new AccountAuditEntity();
        row.setActorAccountId(actor);
        row.setTargetAccountId(target);
        row.setActionCode(action);
        row.setResult(result);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            row.setIpAddress(truncate(request.getRemoteAddr(), 45));
            row.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        }
        mapper.insertAudit(row);
    }

    private static String truncate(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}

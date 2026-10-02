package com.starrainnotes.account.service;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AccountAuditService {

    private final AccountMapper mapper;

    public AccountAuditService(AccountMapper mapper) {
        this.mapper = mapper;
    }

    public void success(Long actor, Long target, String action) {
        record(actor, target, action, "SUCCESS");
    }

    public void failed(Long actor, Long target, String action) {
        record(actor, target, action, "FAILED");
    }

    private void record(Long actor, Long target, String action, String result) {
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


package com.starrainnotes.account.audit;

import com.starrainnotes.account.service.VerificationCodeService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogMapper mapper;

    public AuditLogService(AuditLogMapper mapper) { this.mapper = mapper; }

    public void record(Long actorId, String action, String targetType, Long targetId,
                       String result, String ip, String userAgent, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setResult(result);
        log.setRequestIpHash(ip == null ? null : VerificationCodeService.sha256(ip));
        log.setUserAgentSummary(truncate(userAgent, 200));
        log.setMetadataJson(metadata);
        mapper.insert(log);
    }

    public java.util.List<AuditLog> list(int page, int pageSize) {
        return list(page, pageSize, null);
    }

    public java.util.List<AuditLog> list(int page, int pageSize, String action) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(page, 1) - 1) * size;
        var query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>();
        if (action != null && !action.isBlank()) {
            query.eq(AuditLog::getAction, action.trim());
        }
        return mapper.selectList(query.orderByDesc(AuditLog::getId).last("LIMIT " + size + " OFFSET " + offset));
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

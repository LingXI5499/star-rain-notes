package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;

public record AccountAuditVO(String id, String actorAccountId, String targetAccountId,
        String actionCode, String result, String ipAddress, LocalDateTime createdAt) {
}

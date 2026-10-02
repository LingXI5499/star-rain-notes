package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminAccountVO(String id, String username, String email, String displayName,
        String status, Set<String> roles, LocalDateTime createdAt) {
}

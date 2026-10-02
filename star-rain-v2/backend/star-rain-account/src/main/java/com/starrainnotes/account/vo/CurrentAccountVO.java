package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;

public record CurrentAccountVO(String id, String username, String email, String displayName,
        String status, Set<String> roles, Set<String> permissions, boolean emailVerified) {
}

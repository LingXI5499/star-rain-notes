package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;

public record PermissionVO(String id, String code, String resource, String action,
        String name, String status) {
}

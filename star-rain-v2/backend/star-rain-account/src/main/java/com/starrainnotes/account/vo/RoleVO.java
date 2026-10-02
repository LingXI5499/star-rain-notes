package com.starrainnotes.account.vo;

import java.util.Set;

public record RoleVO(String id, String code, String name, String status, Set<String> permissionIds) {
}

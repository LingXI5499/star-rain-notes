package com.starrainnotes.account.api;

/**
 * 其他模块查询账户权限的唯一入口。
 */
public interface PermissionQueryApi {

    boolean hasPermission(Long accountId, String permissionCode);
}

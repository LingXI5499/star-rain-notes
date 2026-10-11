/*
 * PermissionQueryApi：账号模块的跨模块接口文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：hasPermission（判断是否具备权限）。
 */

package com.starrainnotes.account.api;

/*
 * 其他模块查询账户权限的唯一入口。
 */
/**
 * 权限查询参数接口，属于账号模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface PermissionQueryApi {

    /**
     * 判断是否具备权限。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param permissionCode 权限的业务代码；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean hasPermission(Long accountId, String permissionCode);
}

/*
 * ReviewPermissions：审核模块的常量文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.review.constant;

/*
 * Review 模块声明的业务权限语义。
 *
 * 权限与角色的对应关系由数据库迁移预置，Review 只声明自己的权限码，
 * 不直接操作 Account 的私表，也不替 Account 决定谁能拿到这些权限。
 *
 * 查看类权限同时授予 ADMIN 与 SUPER_ADMIN；
 * 决策类权限当前只授予 SUPER_ADMIN（REV-004 / REV-005 的 Actor 就是 Super Admin）。
 */
/**
 * 审核权限集合，属于审核模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class ReviewPermissions {

    /**
     * 读取。
     */
    public static final String READ = "review:read";
    /**
     * 读取。
     */
    public static final String HISTORY_READ = "review:history-read";
    /**
     * APPROVE。
     */
    public static final String APPROVE = "review:approve";
    /**
     * REJECT。
     */
    public static final String REJECT = "review:reject";

    /**
     * 构造ReviewPermissions，接收本类使用的依赖或初始化参数。
     */
    private ReviewPermissions() {
    }
}

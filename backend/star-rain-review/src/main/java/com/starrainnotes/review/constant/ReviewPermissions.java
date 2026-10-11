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
public final class ReviewPermissions {

    public static final String READ = "review:read";
    public static final String HISTORY_READ = "review:history-read";
    public static final String APPROVE = "review:approve";
    public static final String REJECT = "review:reject";

    private ReviewPermissions() {
    }
}

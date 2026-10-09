package com.starrainnotes.blog.constant;

/*
 * Blog 模块声明的业务权限语义。
 *
 * 权限与角色的对应关系由数据库迁移 V2_007__blog.sql 预置：
 * Blog 只声明自己的权限码，不直接操作 Account 的私表，也不替 Account 决定谁能拿到这些权限。
 *
 * 注意：V2 的博客是个人内容域，这五个权限当前只授予 SUPER_ADMIN，
 * 刻意不授予 ADMIN（教程内容协作管理员），所以后台接口不能只要求“已认证”。
 */
public final class BlogPermissions {

    public static final String READ_ADMIN = "blog:read-admin";
    public static final String EDIT = "blog:edit";
    public static final String PUBLISH = "blog:publish";
    public static final String WITHDRAW = "blog:withdraw";
    public static final String TAXONOMY_MANAGE = "blog:taxonomy-manage";

    private BlogPermissions() {
    }
}

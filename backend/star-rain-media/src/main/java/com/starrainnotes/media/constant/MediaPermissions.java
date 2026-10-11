package com.starrainnotes.media.constant;

/*
 * Media 模块声明的业务权限语义。
 *
 * 权限与角色的对应关系由数据库迁移预置，Media 只声明自己的权限码，
 * 不直接操作 Account 的私表，也不替 Account 决定谁能拿到这些权限。
 */
public final class MediaPermissions {

    public static final String READ = "media:read";
    public static final String UPLOAD = "media:upload";
    public static final String ARCHIVE = "media:archive";
    public static final String RESTORE = "media:restore";
    public static final String ACCESS_MANAGE = "media:access-manage";
    public static final String REFERENCE_READ = "media:reference-read";

    private MediaPermissions() {
    }
}

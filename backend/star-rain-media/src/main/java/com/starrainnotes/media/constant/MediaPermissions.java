/*
 * MediaPermissions：媒体模块的常量文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.media.constant;

/*
 * Media 模块声明的业务权限语义。
 *
 * 权限与角色的对应关系由数据库迁移预置，Media 只声明自己的权限码，
 * 不直接操作 Account 的私表，也不替 Account 决定谁能拿到这些权限。
 */
/**
 * 媒体权限集合，属于媒体模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class MediaPermissions {

    /**
     * 读取。
     */
    public static final String READ = "media:read";
    /**
     * 上传。
     */
    public static final String UPLOAD = "media:upload";
    /**
     * ARCHIVE。
     */
    public static final String ARCHIVE = "media:archive";
    /**
     * RESTORE。
     */
    public static final String RESTORE = "media:restore";
    /**
     * MANAGE。
     */
    public static final String ACCESS_MANAGE = "media:access-manage";
    /**
     * 读取。
     */
    public static final String REFERENCE_READ = "media:reference-read";

    /**
     * 构造MediaPermissions，接收本类使用的依赖或初始化参数。
     */
    private MediaPermissions() {
    }
}

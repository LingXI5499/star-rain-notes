/*
 * MediaStatus：媒体模块的枚举文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.media.enumeration;

/*
 * 媒体生命周期状态。
 *
 * ACTIVE → ARCHIVED 是唯一正式流转；V2 不建立 DELETED / PURGED 常驻状态，
 * 物理清除属于后续独立运维动作，也不提供 force delete。
 */
/**
 * 媒体状态，属于媒体模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum MediaStatus {
    /**
     * 有效。
     */
    ACTIVE,
    /**
     * 已归档。
     */
    ARCHIVED;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    /**
     * 代码。
     */
    public static final String ACTIVE_CODE = "ACTIVE";
    /**
     * 代码。
     */
    public static final String ARCHIVED_CODE = "ARCHIVED";
}

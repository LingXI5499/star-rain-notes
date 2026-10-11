package com.starrainnotes.media.enumeration;

/*
 * 媒体生命周期状态。
 *
 * ACTIVE → ARCHIVED 是唯一正式流转；V2 不建立 DELETED / PURGED 常驻状态，
 * 物理清除属于后续独立运维动作，也不提供 force delete。
 */
public enum MediaStatus {
    ACTIVE,
    ARCHIVED;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    public static final String ACTIVE_CODE = "ACTIVE";
    public static final String ARCHIVED_CODE = "ARCHIVED";
}

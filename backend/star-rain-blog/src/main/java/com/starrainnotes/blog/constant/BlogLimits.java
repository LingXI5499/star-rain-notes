/*
 * BlogLimits：博客模块的常量文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.blog.constant;

/*
 * 博客各字段的长度上限。
 *
 * 数值与 V2_007__blog.sql 的列定义一一对应：Service 先拒绝超长输入，
 * 目的是让调用方拿到明确的 BLOG_* 业务错误，而不是 MySQL 的
 * “Data too long for column”变成 500。改列宽时必须同步改这里。
 */
/**
 * 博客Limits，属于博客模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class BlogLimits {

    /**
     * LENGTH。
     * 初始值为 <code>180</code>。
     */
    public static final int POST_SLUG_MAX_LENGTH = 180;
    /**
     * LENGTH。
     * 初始值为 <code>255</code>。
     */
    public static final int POST_TITLE_MAX_LENGTH = 255;
    /**
     * LENGTH。
     * 初始值为 <code>1000</code>。
     */
    public static final int POST_SUMMARY_MAX_LENGTH = 1000;

    /**
     * LENGTH。
     * 初始值为 <code>100</code>。
     */
    public static final int TAG_SLUG_MAX_LENGTH = 100;
    /**
     * LENGTH。
     * 初始值为 <code>100</code>。
     */
    public static final int TAG_NAME_MAX_LENGTH = 100;
    /**
     * LENGTH。
     * 初始值为 <code>500</code>。
     */
    public static final int TAG_DESCRIPTION_MAX_LENGTH = 500;

    /**
     * LENGTH。
     * 初始值为 <code>120</code>。
     */
    public static final int TOPIC_SLUG_MAX_LENGTH = 120;
    /**
     * LENGTH。
     * 初始值为 <code>160</code>。
     */
    public static final int TOPIC_NAME_MAX_LENGTH = 160;
    /**
     * LENGTH。
     * 初始值为 <code>1000</code>。
     */
    public static final int TOPIC_DESCRIPTION_MAX_LENGTH = 1000;

    /**
     * 构造BlogLimits，接收本类使用的依赖或初始化参数。
     */
    private BlogLimits() {
    }
}

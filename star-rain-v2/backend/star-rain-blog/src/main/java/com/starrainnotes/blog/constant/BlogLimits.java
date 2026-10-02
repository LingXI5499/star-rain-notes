package com.starrainnotes.blog.constant;

/*
 * 博客各字段的长度上限。
 *
 * 数值与 V2_007__blog.sql 的列定义一一对应：Service 先拒绝超长输入，
 * 目的是让调用方拿到明确的 BLOG_* 业务错误，而不是 MySQL 的
 * “Data too long for column”变成 500。改列宽时必须同步改这里。
 */
public final class BlogLimits {

    public static final int POST_SLUG_MAX_LENGTH = 180;
    public static final int POST_TITLE_MAX_LENGTH = 255;
    public static final int POST_SUMMARY_MAX_LENGTH = 1000;

    public static final int TAG_SLUG_MAX_LENGTH = 100;
    public static final int TAG_NAME_MAX_LENGTH = 100;
    public static final int TAG_DESCRIPTION_MAX_LENGTH = 500;

    public static final int TOPIC_SLUG_MAX_LENGTH = 120;
    public static final int TOPIC_NAME_MAX_LENGTH = 160;
    public static final int TOPIC_DESCRIPTION_MAX_LENGTH = 1000;

    private BlogLimits() {
    }
}

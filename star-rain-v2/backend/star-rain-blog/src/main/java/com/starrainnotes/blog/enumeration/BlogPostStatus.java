package com.starrainnotes.blog.enumeration;

import com.starrainnotes.blog.exception.BlogQueryInvalidException;
import java.util.Locale;

/*
 * 文章发布状态。
 *
 * DRAFT → PUBLISHED ↔ WITHDRAWN 是唯一正式流转：
 * 新建文章一律 DRAFT，只有 Super Admin 能发布，撤回后仍保留全部内容与媒体引用，
 * 恢复即回到公开内容集合，不需要重新走一次编辑流程。
 *
 * 不存在 DELETED 常驻状态：删除是 DRAFT / WITHDRAWN 才允许的物理动作，
 * 公开内容必须先撤回，避免已传播的 URL 直接 404。
 */
public enum BlogPostStatus {
    DRAFT,
    PUBLISHED,
    WITHDRAWN;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    public static final String DRAFT_CODE = "DRAFT";
    public static final String PUBLISHED_CODE = "PUBLISHED";
    public static final String WITHDRAWN_CODE = "WITHDRAWN";

    // 严格解析：非法值抛 BLOG_QUERY_INVALID，用于查询筛选与状态判断
    public static BlogPostStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BlogQueryInvalidException("status 不能为空");
        }
        try {
            return BlogPostStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BlogQueryInvalidException("status 只能是 DRAFT/PUBLISHED/WITHDRAWN");
        }
    }

    // 宽松解析：空白表示不筛选，非法值返回 null 由调用方决定拒绝还是忽略
    public static BlogPostStatus of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BlogPostStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

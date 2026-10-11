/*
 * BlogPostStatus：博客模块的枚举文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 主要协作类型：BlogQueryInvalidException。
 * 阅读入口：parse（解析博客文章状态）；of（所属处理入口）。
 */

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
/**
 * 博客文章状态，属于博客模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum BlogPostStatus {
    /**
     * 草稿。
     */
    DRAFT,
    /**
     * 已发布。
     */
    PUBLISHED,
    /**
     * 已撤回。
     */
    WITHDRAWN;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    /**
     * 代码。
     */
    public static final String DRAFT_CODE = "DRAFT";
    /**
     * 代码。
     */
    public static final String PUBLISHED_CODE = "PUBLISHED";
    /**
     * 代码。
     */
    public static final String WITHDRAWN_CODE = "WITHDRAWN";

    // 严格解析：非法值抛 BLOG_QUERY_INVALID，用于查询筛选与状态判断
    /**
     * 解析博客文章状态。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>BlogPostStatus.valueOf</code>、<code>value.trim</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostStatus</code> 的处理结果
     */
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
    /**
     * 所属处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>BlogPostStatus.valueOf</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostStatus</code> 的处理结果
     */
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

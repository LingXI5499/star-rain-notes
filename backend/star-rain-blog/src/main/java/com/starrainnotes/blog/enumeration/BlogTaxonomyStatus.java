/*
 * BlogTaxonomyStatus：博客模块的枚举文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：of（所属处理入口）。
 */

package com.starrainnotes.blog.enumeration;

import java.util.Locale;

/*
 * Tag 与 Topic 共用的启用状态。
 *
 * 两者都只有 ENABLED / DISABLED，没有 DELETED：
 * 已经被文章绑定的 Tag 或承载成员的 Topic 不允许物理删除，
 * 否则历史文章会指向一个不存在的分类，归档浏览随即失真。
 * DISABLED 只表示“不再接受新绑定 / 不在前台入口展示”，历史关系与专题顺序全部保留。
 */
/**
 * 博客Taxonomy状态，属于博客模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum BlogTaxonomyStatus {
    /**
     * 已启用。
     */
    ENABLED,
    /**
     * 已禁用。
     */
    DISABLED;

    /**
     * 代码。
     */
    public static final String ENABLED_CODE = "ENABLED";
    /**
     * 代码。
     */
    public static final String DISABLED_CODE = "DISABLED";

    // 宽松解析：空白或非法值返回 null，让上层自己决定默认值
    /**
     * 所属处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>BlogTaxonomyStatus.valueOf</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>BlogTaxonomyStatus</code> 的处理结果
     */
    public static BlogTaxonomyStatus of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BlogTaxonomyStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

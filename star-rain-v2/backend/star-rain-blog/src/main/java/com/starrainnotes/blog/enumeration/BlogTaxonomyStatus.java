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
public enum BlogTaxonomyStatus {
    ENABLED,
    DISABLED;

    public static final String ENABLED_CODE = "ENABLED";
    public static final String DISABLED_CODE = "DISABLED";

    // 宽松解析：空白或非法值返回 null，让上层自己决定默认值
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

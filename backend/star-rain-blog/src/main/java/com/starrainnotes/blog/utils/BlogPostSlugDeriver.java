/*
 * BlogPostSlugDeriver：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：BlogLimits。
 * 阅读入口：derive（推导博客文章固定链接标识Deriver）；withSuffix（包含Suffix处理入口）。
 */

package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.constant.BlogLimits;

/* 标题派生的 slug 只在创建时确定；之后改标题不改已有公开地址。 */
/**
 * 博客文章固定链接标识Deriver，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogPostSlugDeriver {

    /**
     * 构造BlogPostSlugDeriver，接收本类使用的依赖或初始化参数。
     */
    private BlogPostSlugDeriver() {
    }

    /**
     * 推导博客文章固定链接标识Deriver。
     * 关键处理步骤调用 <code>BlogSlugDeriver.derive</code>。
     *
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String derive(String title) {
        return BlogSlugDeriver.derive(title, "post", BlogLimits.POST_SLUG_MAX_LENGTH);
    }

    /**
     * 包含Suffix处理入口。
     * 关键处理步骤调用 <code>BlogSlugDeriver.withSuffix</code>。
     *
     * @param base 基础；类型为 <code>String</code>
     * @param ordinal 序号；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String withSuffix(String base, int ordinal) {
        return BlogSlugDeriver.withSuffix(base, ordinal, BlogLimits.POST_SLUG_MAX_LENGTH);
    }
}

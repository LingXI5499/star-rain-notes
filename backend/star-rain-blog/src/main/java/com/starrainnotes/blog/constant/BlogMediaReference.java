/*
 * BlogMediaReference：博客模块的常量文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.blog.constant;

/*
 * Blog 在 Media 侧登记引用时使用的来源标识。
 *
 * sourceModule 必须与 usageCode 的前缀一致（blog.cover / blog.content），
 * 否则 MediaReferenceApi 会以 MEDIA_REFERENCE_INVALID 拒绝，
 * 因此这两个字面量只在这里出现，业务代码一律引用常量。
 */
/**
 * 博客媒体引用，属于博客模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class BlogMediaReference {

    /**
     * MODULE。
     */
    public static final String SOURCE_MODULE = "BLOG";
    /**
     * 文章。
     */
    public static final String SOURCE_TYPE_POST = "POST";

    /**
     * 构造BlogMediaReference，接收本类使用的依赖或初始化参数。
     */
    private BlogMediaReference() {
    }
}

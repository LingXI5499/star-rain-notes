package com.starrainnotes.blog.constant;

/*
 * Blog 在 Media 侧登记引用时使用的来源标识。
 *
 * sourceModule 必须与 usageCode 的前缀一致（blog.cover / blog.content），
 * 否则 MediaReferenceApi 会以 MEDIA_REFERENCE_INVALID 拒绝，
 * 因此这两个字面量只在这里出现，业务代码一律引用常量。
 */
public final class BlogMediaReference {

    public static final String SOURCE_MODULE = "BLOG";
    public static final String SOURCE_TYPE_POST = "POST";

    private BlogMediaReference() {
    }
}

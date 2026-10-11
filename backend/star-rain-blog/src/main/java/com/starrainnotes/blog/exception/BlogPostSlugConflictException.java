package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章 slug 已被占用。
 *
 * slug 是公开 URL 的一部分，不能重复；数据库唯一键是最终防线，
 * 这里用于把 DuplicateKeyException 转成可预期的业务错误。
 * 错误码 BLOG_POST_SLUG_CONFLICT，HTTP 409。
 */
public class BlogPostSlugConflictException extends ApiException {

    public BlogPostSlugConflictException() {
        super("BLOG_POST_SLUG_CONFLICT", "文章 slug 已存在", 409);
    }
}

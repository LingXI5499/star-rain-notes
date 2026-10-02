package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签 slug 重复。
 *
 * 错误码 BLOG_TAG_SLUG_CONFLICT，HTTP 409。
 */
public class BlogTagSlugConflictException extends ApiException {

    public BlogTagSlugConflictException() {
        super("BLOG_TAG_SLUG_CONFLICT", "标签 slug 已存在", 409);
    }
}

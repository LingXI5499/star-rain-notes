package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章 slug 格式非法。
 *
 * 只允许小写字母、数字与中划线：slug 会出现在 URL 里，
 * 允许大写或中文会让同一个 slug 出现多种可访问写法，造成重复内容。
 * 错误码 BLOG_POST_SLUG_INVALID，HTTP 400。
 */
public class BlogPostSlugInvalidException extends ApiException {

    public BlogPostSlugInvalidException(String message) {
        super("BLOG_POST_SLUG_INVALID", message, 400);
    }
}

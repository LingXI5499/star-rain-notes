package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章标题非法（为空或超长）。
 *
 * 错误码 BLOG_POST_TITLE_INVALID，HTTP 400。
 */
public class BlogPostTitleInvalidException extends ApiException {

    public BlogPostTitleInvalidException(String message) {
        super("BLOG_POST_TITLE_INVALID", message, 400);
    }
}

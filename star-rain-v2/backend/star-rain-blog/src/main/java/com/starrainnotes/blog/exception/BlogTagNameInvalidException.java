package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签名为空或超长。
 *
 * 错误码 BLOG_TAG_NAME_INVALID，HTTP 400。
 */
public class BlogTagNameInvalidException extends ApiException {

    public BlogTagNameInvalidException(String message) {
        super("BLOG_TAG_NAME_INVALID", message, 400);
    }
}

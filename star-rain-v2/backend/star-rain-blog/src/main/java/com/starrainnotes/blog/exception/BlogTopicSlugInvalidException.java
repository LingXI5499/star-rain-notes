package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题 slug 格式非法。
 *
 * 错误码 BLOG_TOPIC_SLUG_INVALID，HTTP 400。
 */
public class BlogTopicSlugInvalidException extends ApiException {

    public BlogTopicSlugInvalidException(String message) {
        super("BLOG_TOPIC_SLUG_INVALID", message, 400);
    }
}

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题名为空或超长。
 *
 * 错误码 BLOG_TOPIC_NAME_INVALID，HTTP 400。
 */
public class BlogTopicNameInvalidException extends ApiException {

    public BlogTopicNameInvalidException(String message) {
        super("BLOG_TOPIC_NAME_INVALID", message, 400);
    }
}

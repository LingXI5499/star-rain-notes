package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题说明超长。
 *
 * 错误码 BLOG_TOPIC_DESCRIPTION_INVALID，HTTP 400。
 */
public class BlogTopicDescriptionInvalidException extends ApiException {

    public BlogTopicDescriptionInvalidException(String message) {
        super("BLOG_TOPIC_DESCRIPTION_INVALID", message, 400);
    }
}

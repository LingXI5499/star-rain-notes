package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题不存在。
 *
 * 错误码 BLOG_TOPIC_NOT_FOUND，HTTP 404。
 */
public class BlogTopicNotFoundException extends ApiException {

    public BlogTopicNotFoundException() {
        super("BLOG_TOPIC_NOT_FOUND", "专题不存在", 404);
    }
}

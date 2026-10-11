package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签不存在。
 *
 * 错误码 BLOG_TAG_NOT_FOUND，HTTP 404。
 */
public class BlogTagNotFoundException extends ApiException {

    public BlogTagNotFoundException() {
        super("BLOG_TAG_NOT_FOUND", "标签不存在", 404);
    }
}

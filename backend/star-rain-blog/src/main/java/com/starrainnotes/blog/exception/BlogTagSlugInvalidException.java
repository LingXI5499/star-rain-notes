package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签 slug 格式非法。
 *
 * 错误码 BLOG_TAG_SLUG_INVALID，HTTP 400。
 * 与 BLOG_TAG_SLUG_CONFLICT 分开：格式错是调用方写错了，冲突是写对了但重名，
 * 前端对这两种情况的提示完全不同。
 */
public class BlogTagSlugInvalidException extends ApiException {

    public BlogTagSlugInvalidException(String message) {
        super("BLOG_TAG_SLUG_INVALID", message, 400);
    }
}

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 正文为空。
 *
 * 允许草稿暂时没有正文，但发布前必须非空：空文章进入公开列表没有意义。
 * 错误码 BLOG_POST_CONTENT_EMPTY，HTTP 400。
 */
public class BlogPostContentEmptyException extends ApiException {

    public BlogPostContentEmptyException() {
        super("BLOG_POST_CONTENT_EMPTY", "正文不能为空", 400);
    }
}

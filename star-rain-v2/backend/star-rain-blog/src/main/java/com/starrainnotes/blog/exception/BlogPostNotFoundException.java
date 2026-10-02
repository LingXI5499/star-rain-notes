package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章不存在。
 *
 * 公开接口对 DRAFT / WITHDRAWN 也返回这个错误：对外不能区分“不存在”和“未公开”，
 * 否则可以通过错误码差异探测出还没发布的文章。
 * 错误码 BLOG_POST_NOT_FOUND，HTTP 404。
 */
public class BlogPostNotFoundException extends ApiException {

    public BlogPostNotFoundException() {
        super("BLOG_POST_NOT_FOUND", "文章不存在", 404);
    }
}

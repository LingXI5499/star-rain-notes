package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章不在该专题里。
 *
 * 移出与排序都要求成员关系真实存在：
 * 排序请求里出现非成员 ID 时直接拒绝，避免“静默忽略”导致前端看到的顺序与库里不一致。
 * 错误码 BLOG_TOPIC_POST_NOT_FOUND，HTTP 404。
 */
public class BlogTopicPostNotFoundException extends ApiException {

    public BlogTopicPostNotFoundException() {
        super("BLOG_TOPIC_POST_NOT_FOUND", "文章不在该专题中", 404);
    }
}

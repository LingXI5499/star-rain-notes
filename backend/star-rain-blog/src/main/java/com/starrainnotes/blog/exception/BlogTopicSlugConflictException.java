package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题 slug 重复。
 *
 * 与 Tag 不同，Topic 的 name 允许重复：专题靠 slug 定位与排序，
 * 名字是给人看的说明，重复名字不会让归档入口分裂。
 * 错误码 BLOG_TOPIC_SLUG_CONFLICT，HTTP 409。
 */
public class BlogTopicSlugConflictException extends ApiException {

    public BlogTopicSlugConflictException() {
        super("BLOG_TOPIC_SLUG_CONFLICT", "专题 slug 已存在", 409);
    }
}

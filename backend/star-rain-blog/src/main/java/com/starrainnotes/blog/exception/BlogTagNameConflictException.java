package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签名重复。
 *
 * 标签名唯一是刻意约束（Topic 名则允许重复）：同名不同 ID 的标签会让归档浏览
 * 把同一批文章拆到两个入口，读者无法判断该点哪一个。
 * 错误码 BLOG_TAG_NAME_CONFLICT，HTTP 409。
 */
public class BlogTagNameConflictException extends ApiException {

    public BlogTagNameConflictException() {
        super("BLOG_TAG_NAME_CONFLICT", "标签名已存在", 409);
    }
}

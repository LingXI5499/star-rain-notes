package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章状态不允许当前操作。
 *
 * 例：DRAFT 不能撤回、PUBLISHED 不能重复发布、已并发变化的条件更新影响 0 行。
 * 与 BLOG_ACCESS_DENIED 分开：这里是“状态不对”，不是“权限不够”。
 * 错误码 BLOG_POST_STATE_INVALID，HTTP 409。
 */
public class BlogPostStateInvalidException extends ApiException {

    public BlogPostStateInvalidException(String message) {
        super("BLOG_POST_STATE_INVALID", message, 409);
    }
}

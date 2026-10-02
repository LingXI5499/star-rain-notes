package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签说明超长。
 *
 * 与 BLOG_QUERY_INVALID 分开：这是写入参数的问题，不是查询条件的问题，
 * 前端要提示的是“你填的说明太长”，不是“你查的参数不对”。
 * 错误码 BLOG_TAG_DESCRIPTION_INVALID，HTTP 400。
 */
public class BlogTagDescriptionInvalidException extends ApiException {

    public BlogTagDescriptionInvalidException(String message) {
        super("BLOG_TAG_DESCRIPTION_INVALID", message, 400);
    }
}

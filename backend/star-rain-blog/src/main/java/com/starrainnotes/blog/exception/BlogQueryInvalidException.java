package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 查询或筛选参数非法。
 *
 * 查询接口不写 jakarta.validation 注解：合法性由 Service 显式校验，
 * 这样错误码固定为 BLOG_QUERY_INVALID，而不是落进全局处理器变成笼统的 INVALID_REQUEST，
 * 也不会因为越界分页让数据库报错后变成 500。
 * 错误码 BLOG_QUERY_INVALID，HTTP 400。
 */
public class BlogQueryInvalidException extends ApiException {

    public BlogQueryInvalidException(String message) {
        super("BLOG_QUERY_INVALID", message, 400);
    }
}

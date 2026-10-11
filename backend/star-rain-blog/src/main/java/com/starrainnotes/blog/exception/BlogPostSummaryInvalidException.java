package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章摘要超出长度约束。
 *
 * 摘要会进入列表卡片与 meta description，长度上限 1000 与数据库列一致，
 * 在 Service 里先拒绝，避免 MySQL 以 “Data too long” 抛 500。
 * 错误码 BLOG_POST_SUMMARY_INVALID，HTTP 400。
 */
public class BlogPostSummaryInvalidException extends ApiException {

    public BlogPostSummaryInvalidException(String message) {
        super("BLOG_POST_SUMMARY_INVALID", message, 400);
    }
}

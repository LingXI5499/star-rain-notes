/*
 * BlogPostSummaryInvalidException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章摘要超出长度约束。
 *
 * 摘要会进入列表卡片与 meta description，长度上限 1000 与数据库列一致，
 * 在 Service 里先拒绝，避免 MySQL 以 “Data too long” 抛 500。
 * 错误码 BLOG_POST_SUMMARY_INVALID，HTTP 400。
 */
/**
 * 博客文章摘要无效异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class BlogPostSummaryInvalidException extends ApiException {

    /**
     * 构造BlogPostSummaryInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public BlogPostSummaryInvalidException(String message) {
        super("BLOG_POST_SUMMARY_INVALID", message, 400);
    }
}

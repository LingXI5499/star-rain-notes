/*
 * BlogPostSlugConflictException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章 slug 已被占用。
 *
 * slug 是公开 URL 的一部分，不能重复；数据库唯一键是最终防线，
 * 这里用于把 DuplicateKeyException 转成可预期的业务错误。
 * 错误码 BLOG_POST_SLUG_CONFLICT，HTTP 409。
 */
/**
 * 博客文章固定链接标识冲突异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_POST_SLUG_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：文章 slug 已存在。
 */
public class BlogPostSlugConflictException extends ApiException {

    /**
     * 构造BlogPostSlugConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogPostSlugConflictException() {
        super("BLOG_POST_SLUG_CONFLICT", "文章 slug 已存在", 409);
    }
}

/*
 * BlogPostSlugInvalidException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章 slug 格式非法。
 *
 * 只允许小写字母、数字与中划线：slug 会出现在 URL 里，
 * 允许大写或中文会让同一个 slug 出现多种可访问写法，造成重复内容。
 * 错误码 BLOG_POST_SLUG_INVALID，HTTP 400。
 */
/**
 * 博客文章固定链接标识无效异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class BlogPostSlugInvalidException extends ApiException {

    /**
     * 构造BlogPostSlugInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public BlogPostSlugInvalidException(String message) {
        super("BLOG_POST_SLUG_INVALID", message, 400);
    }
}

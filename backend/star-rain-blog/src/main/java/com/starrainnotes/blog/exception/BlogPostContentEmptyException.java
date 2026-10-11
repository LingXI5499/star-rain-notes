/*
 * BlogPostContentEmptyException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 正文为空。
 *
 * 允许草稿暂时没有正文，但发布前必须非空：空文章进入公开列表没有意义。
 * 错误码 BLOG_POST_CONTENT_EMPTY，HTTP 400。
 */
/**
 * 博客文章内容空白异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_POST_CONTENT_EMPTY</code>；HTTP 状态：<code>400</code>。
 * 公开提示：正文不能为空。
 */
public class BlogPostContentEmptyException extends ApiException {

    /**
     * 构造BlogPostContentEmptyException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogPostContentEmptyException() {
        super("BLOG_POST_CONTENT_EMPTY", "正文不能为空", 400);
    }
}

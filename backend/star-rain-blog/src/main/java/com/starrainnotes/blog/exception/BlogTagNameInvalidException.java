/*
 * BlogTagNameInvalidException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签名为空或超长。
 *
 * 错误码 BLOG_TAG_NAME_INVALID，HTTP 400。
 */
/**
 * 博客标签名称无效异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class BlogTagNameInvalidException extends ApiException {

    /**
     * 构造BlogTagNameInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public BlogTagNameInvalidException(String message) {
        super("BLOG_TAG_NAME_INVALID", message, 400);
    }
}

/*
 * BlogTagDescriptionInvalidException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签说明超长。
 *
 * 与 BLOG_QUERY_INVALID 分开：这是写入参数的问题，不是查询条件的问题，
 * 前端要提示的是“你填的说明太长”，不是“你查的参数不对”。
 * 错误码 BLOG_TAG_DESCRIPTION_INVALID，HTTP 400。
 */
/**
 * 博客标签描述无效异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class BlogTagDescriptionInvalidException extends ApiException {

    /**
     * 构造BlogTagDescriptionInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public BlogTagDescriptionInvalidException(String message) {
        super("BLOG_TAG_DESCRIPTION_INVALID", message, 400);
    }
}

/*
 * BlogTagSlugConflictException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签 slug 重复。
 *
 * 错误码 BLOG_TAG_SLUG_CONFLICT，HTTP 409。
 */
/**
 * 博客标签固定链接标识冲突异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TAG_SLUG_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：标签 slug 已存在。
 */
public class BlogTagSlugConflictException extends ApiException {

    /**
     * 构造BlogTagSlugConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTagSlugConflictException() {
        super("BLOG_TAG_SLUG_CONFLICT", "标签 slug 已存在", 409);
    }
}

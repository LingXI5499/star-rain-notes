/*
 * BlogTagNameConflictException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签名重复。
 *
 * 标签名唯一是刻意约束（Topic 名则允许重复）：同名不同 ID 的标签会让归档浏览
 * 把同一批文章拆到两个入口，读者无法判断该点哪一个。
 * 错误码 BLOG_TAG_NAME_CONFLICT，HTTP 409。
 */
/**
 * 博客标签名称冲突异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TAG_NAME_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：标签名已存在。
 */
public class BlogTagNameConflictException extends ApiException {

    /**
     * 构造BlogTagNameConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTagNameConflictException() {
        super("BLOG_TAG_NAME_CONFLICT", "标签名已存在", 409);
    }
}

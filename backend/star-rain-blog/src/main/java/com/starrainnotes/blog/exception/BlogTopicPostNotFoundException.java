/*
 * BlogTopicPostNotFoundException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章不在该专题里。
 *
 * 移出与排序都要求成员关系真实存在：
 * 排序请求里出现非成员 ID 时直接拒绝，避免“静默忽略”导致前端看到的顺序与库里不一致。
 * 错误码 BLOG_TOPIC_POST_NOT_FOUND，HTTP 404。
 */
/**
 * 博客专题文章不存在异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TOPIC_POST_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：文章不在该专题中。
 */
public class BlogTopicPostNotFoundException extends ApiException {

    /**
     * 构造BlogTopicPostNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTopicPostNotFoundException() {
        super("BLOG_TOPIC_POST_NOT_FOUND", "文章不在该专题中", 404);
    }
}

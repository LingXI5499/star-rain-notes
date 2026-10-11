/*
 * BlogTopicDisabledException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题已停用，不能再加入新文章。
 *
 * 停用不删除成员关系、也不清空顺序：重新启用后原排序继续有效。
 * 错误码 BLOG_TOPIC_DISABLED，HTTP 409。
 */
/**
 * 博客专题已禁用异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TOPIC_DISABLED</code>；HTTP 状态：<code>409</code>。
 * 公开提示：专题已停用，不能加入文章。
 */
public class BlogTopicDisabledException extends ApiException {

    /**
     * 构造BlogTopicDisabledException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTopicDisabledException() {
        super("BLOG_TOPIC_DISABLED", "专题已停用，不能加入文章", 409);
    }
}

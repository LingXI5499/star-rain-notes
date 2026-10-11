/*
 * BlogTagDisabledException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签已停用，不能再绑定到文章，也不能作为发布前的新绑定。
 *
 * 注意：停用不会删除历史绑定，所以“已绑定的停用标签”不影响文章继续发布，
 * 只有“本次要新绑定的标签是停用状态”才报这个错误。
 * 错误码 BLOG_TAG_DISABLED，HTTP 409。
 */
/**
 * 博客标签已禁用异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TAG_DISABLED</code>；HTTP 状态：<code>409</code>。
 * 公开提示：标签已停用，不能绑定到文章。
 */
public class BlogTagDisabledException extends ApiException {

    /**
     * 构造BlogTagDisabledException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTagDisabledException() {
        super("BLOG_TAG_DISABLED", "标签已停用，不能绑定到文章", 409);
    }
}

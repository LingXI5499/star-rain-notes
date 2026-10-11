/*
 * BlogTopicNotEmptyException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题下还有文章，不能物理删除。
 *
 * Topic 承载人工策展的成员与顺序，因此删除只对**空专题**开放：
 * 有成员时要求调用方先显式移出（移出会压缩序号），
 * 而不是让一次误点把整份策展结果连同关系一起抹掉。
 * 错误码 BLOG_TOPIC_NOT_EMPTY，HTTP 409。
 */
/**
 * 博客专题不空白异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TOPIC_NOT_EMPTY</code>；HTTP 状态：<code>409</code>。
 * 公开提示：专题下还有文章，请先移出全部文章再删除。
 */
public class BlogTopicNotEmptyException extends ApiException {

    /**
     * 构造BlogTopicNotEmptyException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTopicNotEmptyException() {
        super("BLOG_TOPIC_NOT_EMPTY", "专题下还有文章，请先移出全部文章再删除", 409);
    }
}

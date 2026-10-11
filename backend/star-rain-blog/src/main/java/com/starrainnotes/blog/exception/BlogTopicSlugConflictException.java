/*
 * BlogTopicSlugConflictException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题 slug 重复。
 *
 * 与 Tag 不同，Topic 的 name 允许重复：专题靠 slug 定位与排序，
 * 名字是给人看的说明，重复名字不会让归档入口分裂。
 * 错误码 BLOG_TOPIC_SLUG_CONFLICT，HTTP 409。
 */
/**
 * 博客专题固定链接标识冲突异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_TOPIC_SLUG_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：专题 slug 已存在。
 */
public class BlogTopicSlugConflictException extends ApiException {

    /**
     * 构造BlogTopicSlugConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogTopicSlugConflictException() {
        super("BLOG_TOPIC_SLUG_CONFLICT", "专题 slug 已存在", 409);
    }
}

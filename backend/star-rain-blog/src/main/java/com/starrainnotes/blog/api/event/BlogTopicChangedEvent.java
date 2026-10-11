/*
 * BlogTopicChangedEvent：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.blog.api.event;

/** Topic page change published after the blog transaction commits. */
public record BlogTopicChangedEvent(
    /**
     * 专题标识。
     */
    Long topicId,
    /**
     * 上一项固定链接标识。
     */
    String previousSlug,
    /**
     * 公开固定链接的路径标识。
     */
    String slug,
    /**
     * 可见。
     */
    boolean visible) {
}

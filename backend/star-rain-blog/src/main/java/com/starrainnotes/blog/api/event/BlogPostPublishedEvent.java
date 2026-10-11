/*
 * BlogPostPublishedEvent：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.blog.api.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 文章发布事件（BLOG-008）。
 *
 * 在事务提交之后发布：Search / SEO / Site 拿到事件时文章一定已经可见，
 * 否则消费者可能在事务回滚前就去建索引，留下指向不存在文章的索引项。
 *
 * 只带派生数据消费者真正需要的字段，不带 Entity：
 * 事件是跨模块契约，暴露 Entity 会把 Blog 的表结构变成别人的依赖。
 */
/**
 * 博客文章已发布事件，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublishedEvent {

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;

    // 首次发布的时间，恢复场景下就是原发布时间
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
}

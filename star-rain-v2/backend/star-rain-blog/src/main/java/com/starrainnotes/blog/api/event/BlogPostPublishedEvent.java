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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublishedEvent {

    private Long postId;
    private String slug;
    private String title;

    // 首次发布的时间，恢复场景下就是原发布时间
    private LocalDateTime publishedAt;
}

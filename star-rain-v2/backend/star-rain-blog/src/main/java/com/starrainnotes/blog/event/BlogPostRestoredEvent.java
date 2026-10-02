package com.starrainnotes.blog.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 文章恢复事件（BLOG-009）。
 *
 * 与发布事件分开而不是复用：消费者需要区分“新内容要索引”和“旧内容要恢复”，
 * 合用一个事件会逼消费者反查状态，等于把 Blog 的状态机逻辑复制出去。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostRestoredEvent {

    private Long postId;
    private String slug;

    // 恢复后仍然是原来的首次发布时间
    private LocalDateTime publishedAt;
}

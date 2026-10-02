package com.starrainnotes.blog.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 文章撤回事件（BLOG-009）：Search 删索引、SEO 移除快照与 Sitemap 项
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostWithdrawnEvent {

    private Long postId;
    private String slug;
    private LocalDateTime withdrawnAt;
}

package com.starrainnotes.blog.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 模块间文章文档（Search / SEO 重建索引使用）。
 *
 * 带正文与分类 slug：Search 要把正文切词，SEO 要生成公开快照与 Sitemap，
 * 它们不能直接 JOIN Blog 的表，只能通过这个契约拿数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDocument {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;

    private String coverUrl;

    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    private List<String> tagSlugs;
    private List<String> topicSlugs;
}

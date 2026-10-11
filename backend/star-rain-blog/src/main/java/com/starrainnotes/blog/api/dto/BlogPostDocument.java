/*
 * BlogPostDocument：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

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
/**
 * 博客文章文档，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDocument {

    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;

    /**
     * 封面地址。
     */
    private String coverUrl;

    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 标签Slugs。
     */
    private List<String> tagSlugs;
    /**
     * 专题Slugs。
     */
    private List<String> topicSlugs;
}

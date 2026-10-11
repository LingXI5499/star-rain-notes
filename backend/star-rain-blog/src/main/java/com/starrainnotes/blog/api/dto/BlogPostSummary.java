/*
 * BlogPostSummary：博客模块的请求与传输数据文件。
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
 * 模块间文章摘要（Site 首页、Profile 精选内容）。
 *
 * 只包含公开可展示字段，且只对 PUBLISHED 文章生成：
 * 业务模块拿不到 status，也就不会出现“把草稿渲染到首页”这种错误用法。
 */
/**
 * 博客文章摘要，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostSummary {

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
     * 封面地址。
     */
    private String coverUrl;

    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 标签Names。
     */
    private List<String> tagNames;
    /**
     * 专题Names。
     */
    private List<String> topicNames;
}

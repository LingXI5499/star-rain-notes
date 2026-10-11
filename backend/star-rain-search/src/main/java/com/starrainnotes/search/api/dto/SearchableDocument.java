/*
 * SearchableDocument：搜索模块的请求与传输数据文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.search.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 可搜索文档，属于搜索模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class SearchableDocument {
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private Long contentId;
    /**
     * 文档键。
     */
    private String documentKey;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * 可搜索文本。
     */
    private String searchableText;
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 来源更新时间。
     */
    private LocalDateTime sourceUpdatedAt;
}

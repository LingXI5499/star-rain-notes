/*
 * PortfolioPublishedWork：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.portfolio.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品已发布作品，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPublishedWork {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 作品类型。
     */
    private String workType;
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
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
    /**
     * 可搜索文本。
     */
    private String searchableText;
    /**
     * SEO标题。
     */
    private String seoTitle;
    /**
     * SEO描述。
     */
    private String seoDescription;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

/*
 * SeoSourceDocument：公开页面与 SEO模块的请求与传输数据文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.seo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SEO来源文档，属于公开页面与 SEO模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoSourceDocument {
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private Long contentId;
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
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

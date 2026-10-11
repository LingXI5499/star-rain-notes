/*
 * SearchHitVO：搜索模块的响应数据文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.search.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 搜索Hit响应对象，属于搜索模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
public class SearchHitVO {
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private String contentId;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * score。
     */
    private Double score;
}

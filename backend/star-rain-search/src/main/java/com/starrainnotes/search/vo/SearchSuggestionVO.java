/*
 * SearchSuggestionVO：搜索模块的响应数据文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.search.vo;

import lombok.Data;

/**
 * 搜索Suggestion响应对象，属于搜索模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
public class SearchSuggestionVO {
    /**
     * 文本。
     */
    private String text;
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 路由路径。
     */
    private String routePath;
}

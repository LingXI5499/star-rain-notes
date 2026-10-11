/*
 * EnglishSearchDocument：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 英语搜索文档，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class EnglishSearchDocument {
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
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
     * 更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 主题标识。
     */
    private Long themeId;
}

/*
 * ReadingDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：DocumentMetadata。
 */

package com.starrainnotes.english.reading.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import java.time.LocalDateTime;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阅读传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class ReadingDto {
    /**
     * 构造ReadingDto，接收本类使用的依赖或初始化参数。
     */
    private ReadingDto() { }
    /**
     * Rights，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Rights {
        /**
         * rights状态。
         */
        private String rightsStatus;
        /**
         * rightsBasis。
         */
        private String rightsBasis;
        /**
         * 原始作者。
         */
        private String originalAuthor;
        /**
         * licenseNotice。
         */
        private String licenseNotice;
    }

    /**
     * Article，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public static class Article extends DocumentMetadata {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
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
         * translationZhMarkdown。
         */
        private String translationZhMarkdown;
        /**
         * 内容来源。
         */
        private String contentOrigin;
        /**
         * rights。
         */
        private Rights rights;
        /**
         * 来源名称。
         */
        private String sourceName;
        /**
         * 来源地址。
         */
        private String sourceUrl;
        /**
         * 发布状态。
         */
        private String publishStatus;
        private LocalDateTime publishedAt;
        private LocalDateTime updatedAt;
        /**
         * 展示排序值。
         */
        private int sortOrder;
    }

    /**
     * 分页，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        /**
         * 条目集合。
         */
        private List<Article> items;
        /**
         * 符合查询条件的总记录数。
         */
        private long total;
        /**
         * 分页页码。
         */
        private int page;
        /**
         * 大小。
         */
        private int size;
    }

    /**
     * 请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public static class Request extends DocumentMetadata {
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
         * translationZhMarkdown。
         */
        private String translationZhMarkdown;
        /**
         * 内容来源。
         */
        private String contentOrigin;
        /**
         * rights。
         */
        private Rights rights;
        /**
         * 来源名称。
         */
        private String sourceName;
        /**
         * 来源地址。
         */
        private String sourceUrl;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }
}

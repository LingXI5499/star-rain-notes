/*
 * GrammarDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.grammar.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语法传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class GrammarDto {
    /**
     * 构造GrammarDto，接收本类使用的依赖或初始化参数。
     */
    private GrammarDto() { }

    /**
     * Course，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class Course {
        /**
         * 标题。
         */
        private String title;
        /**
         * 副标题。
         */
        private String subtitle;
        /**
         * 摘要。
         */
        private String summary;
        /**
         * 介绍。
         */
        private String introduction;
        /**
         * 路线Markdown。
         */
        private String roadmapMarkdown;
        /**
         * 发布状态。
         */
        private String publishStatus;
    }

    /**
     * 区块，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class Section {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 标题。
         */
        private String title;
        /**
         * 展示排序值。
         */
        private int sortOrder;
        /**
         * lessons。
         */
        private List<Lesson> lessons;
    }

    /**
     * Lesson，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class Lesson {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 区块标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sectionId;
        /**
         * 标题。
         */
        private String title;
        /**
         * 公开固定链接的路径标识。
         */
        private String slug;
        /**
         * 摘要。
         */
        private String summary;
        /**
         * Markdown 格式的正文。
         */
        private String bodyMarkdown;
        /**
         * 发布状态。
         */
        private String publishStatus;
        /**
         * 展示排序值。
         */
        private int sortOrder;
    }

    /**
     * Curriculum，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Curriculum {
        /**
         * course。
         */
        private Course course;
        /**
         * sections。
         */
        private List<Section> sections;
    }

    /**
     * Course请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRequest {
        /**
         * 标题。
         */
        private String title;
        /**
         * 副标题。
         */
        private String subtitle;
        /**
         * 摘要。
         */
        private String summary;
        /**
         * 介绍。
         */
        private String introduction;
        /**
         * 路线Markdown。
         */
        private String roadmapMarkdown;
    }

    /**
     * 区块请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionRequest {
        /**
         * 标题。
         */
        private String title;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }

    /**
     * Lesson请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonRequest {
        /**
         * 区块标识。
         */
        private String sectionId;
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
         * 展示排序值。
         */
        private Integer sortOrder;
    }
}

/*
 * WritingArticleDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：DocumentMetadata。
 */

package com.starrainnotes.english.writing.article.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
/**
 * 写作Article传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class WritingArticleDto {
    /**
     * 构造WritingArticleDto，接收本类使用的依赖或初始化参数。
     */
    private WritingArticleDto() { }
    /**
     * Article，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Article extends DocumentMetadata {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        /**
         * owner账号标识。
         */
        @JsonIgnore private Long ownerAccountId;
    /**
     * 公开Eligible。
     */
    @JsonIgnore private boolean publicEligible;
        /**
         * keywordsJSON。
         */
        @JsonIgnore private String keywordsJson;
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
     * 状态。
     */
    private String state;
    /**
     * visibility。
     */
    private String visibility;
        /**
         * 创建时间。
         */
        private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    }
    /**
     * 请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data @JsonIgnoreProperties(ignoreUnknown=true) public static class Request extends DocumentMetadata {
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
    }
    /**
     * 分页，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     * @param items 条目集合；类型为 <code>List&lt;Article&gt;</code>
     * @param total 符合查询条件的总记录数；类型为 <code>long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     */
    public record Page(
    /**
     * 条目集合。
     */
    List<Article> items,
    /**
     * 符合查询条件的总记录数。
     */
    long total,
    /**
     * 分页页码。
     */
    int page,
    /**
     * 大小。
     */
    int size) { }
}

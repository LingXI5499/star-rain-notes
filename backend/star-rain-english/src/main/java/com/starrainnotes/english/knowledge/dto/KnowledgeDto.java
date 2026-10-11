/*
 * KnowledgeDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
/**
 * 知识传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class KnowledgeDto {
    /**
     * 构造KnowledgeDto，接收本类使用的依赖或初始化参数。
     */
    private KnowledgeDto() { }
    /**
     * Item，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Item {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        /**
         * 类型。
         */
        private String type;
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
         * primary专题标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using=ToStringSerializer.class) private Long primaryTopicId;
        /**
         * 已发布时间。
         */
        private LocalDateTime publishedAt;
    }
    /**
     * 分页，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     * @param items 条目集合；类型为 <code>List&lt;Item&gt;</code>
     * @param total 符合查询条件的总记录数；类型为 <code>long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     */
    public record Page(
    /**
     * 条目集合。
     */
    List<Item> items,
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

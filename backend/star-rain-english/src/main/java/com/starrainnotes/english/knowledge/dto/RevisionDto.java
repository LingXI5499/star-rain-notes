/*
 * RevisionDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;
/**
 * 修订传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class RevisionDto {
    /**
     * 构造RevisionDto，接收本类使用的依赖或初始化参数。
     */
    private RevisionDto() { }
    /**
     * 修订，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Revision {
        /**
         * 修订不含。
         */
        private long revisionNo;
        /**
         * 创建时间。
         */
        private String createdAt;
        /**
         * changeNote。
         */
        private String changeNote;
        /**
         * 快照JSON。
         */
        @JsonIgnore private String snapshotJson;
        /**
         * 快照。
         */
        private Object snapshot;
    }
    /**
     * 请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Request {
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    /**
     * changeNote。
     */
    private String changeNote; }
    /**
     * 分页，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     * @param items 条目集合；类型为 <code>List&lt;Revision&gt;</code>
     * @param total 符合查询条件的总记录数；类型为 <code>long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     */
    public record Page(
    /**
     * 条目集合。
     */
    List<Revision> items,
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

/*
 * ReadingEnhancementDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.reading.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;
/**
 * 阅读Enhancement传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class ReadingEnhancementDto {
    /**
     * 构造ReadingEnhancementDto，接收本类使用的依赖或初始化参数。
     */
    private ReadingEnhancementDto() { }
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
         * 分组键。
         */
        private String groupKey;
    /**
     * language。
     */
    private String language;
    /**
     * paragraph索引。
     */
    private Integer paragraphIndex;
        /**
         * 范围开始。
         */
        private Integer rangeStart;
    /**
     * 范围结束。
     */
    private Integer rangeEnd;
    /**
     * expected文本。
     */
    private String expectedText;
        /**
         * paragraph摘要。
         */
        private String paragraphHash;
    /**
     * 来源Markdown摘要。
     */
    private String sourceMarkdownHash;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
        /**
         * analysisMarkdown。
         */
        private String analysisMarkdown;
    /**
     * 单词。
     */
    private String word;
    /**
     * part所属Speech。
     */
    private String partOfSpeech;
        /**
         * meaningZh。
         */
        private String meaningZh;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    }
    /**
     * 批量，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Batch {
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    /**
     * 条目集合。
     */
    private List<Item> items; }
    /**
     * 结果，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；类型为 <code>Long</code>
     * @param items 条目集合；类型为 <code>List&lt;Item&gt;</code>
     */
    public record Result(
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    Long rowVersion,
    /**
     * 条目集合。
     */
    List<Item> items) { }
    /**
     * 快照，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     * @param article article；类型为 <code>ReadingDto.Article</code>
     * @param alignments alignments；类型为 <code>List&lt;Item&gt;</code>
     * @param annotations annotations；类型为 <code>List&lt;Item&gt;</code>
     * @param vocabulary 词汇；类型为 <code>List&lt;Item&gt;</code>
     */
    public record Snapshot(
    /**
     * article。
     */
    ReadingDto.Article article,
    /**
     * alignments。
     */
    List<Item> alignments,
    /**
     * annotations。
     */
    List<Item> annotations,
    /**
     * 词汇。
     */
    List<Item> vocabulary) { }
}

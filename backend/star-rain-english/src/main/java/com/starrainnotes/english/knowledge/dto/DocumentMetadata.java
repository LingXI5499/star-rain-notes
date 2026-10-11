/*
 * DocumentMetadata：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;
/**
 * 文档元数据，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class DocumentMetadata {
    /**
     * primary专题标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using=ToStringSerializer.class) private Long primaryTopicId;
    /**
     * other专题标识集合。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> otherTopicIds=List.of();
    /**
     * genre标识集合。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> genreIds=List.of();
    /**
     * purpose标识集合。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> purposeIds=List.of();
    /**
     * keywords。
     */
    private List<String> keywords=List.of();
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
}

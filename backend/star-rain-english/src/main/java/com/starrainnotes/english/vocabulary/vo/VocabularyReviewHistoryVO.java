/*
 * VocabularyReviewHistoryVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一条真实复习记录（进度页「最近真实复习」）。
 */
/**
 * 词汇审核历史响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewHistoryVO {
    /**
     * 评分。
     */
    private String rating;
    /**
     * 来源。
     */
    private String source;

    /**
     * 当前业务对象的数据库标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 单词标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    /**
     * 单词。
     */
    private String word;

    /**
     * 审核请求的业务编号。
     */
    private int reviewNumber;

    /**
     * direction。
     */
    private String direction;

    /**
     * 计划调度时间。
     */
    private String scheduledAt;

    /**
     * 复习时间。
     */
    private String reviewedAt;

    /**
     * 下一次复习的间隔，单位为秒。
     */
    private long intervalSeconds;

    /**
     * 调度状态。
     */
    private String timingStatus;
}

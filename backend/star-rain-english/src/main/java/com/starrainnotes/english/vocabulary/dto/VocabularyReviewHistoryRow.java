/*
 * VocabularyReviewHistoryRow：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一条真实复习记录（仓储 -> 服务）。
 */
/**
 * 词汇审核历史行，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewHistoryRow {
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
     */
    private Long id;

    /**
     * 单词标识。
     */
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
    private LocalDateTime scheduledAt;

    /**
     * 复习时间。
     */
    private LocalDateTime reviewedAt;

    /**
     * 下一次复习的间隔，单位为秒。
     */
    private long intervalSeconds;

    /**
     * 调度状态。
     */
    private String timingStatus;
}

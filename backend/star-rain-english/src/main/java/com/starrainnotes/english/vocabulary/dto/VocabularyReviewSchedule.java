/*
 * VocabularyReviewSchedule：英语学习模块的请求与传输数据文件。
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
 * 一次复习的排期结果（层间传输对象：策略 -> 仓储）。
 */
/**
 * 词汇审核Schedule，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewSchedule {

    /* 第几次复习，从 1 开始 */
    /**
     * 审核请求的业务编号。
     */
    private int reviewNumber;

    /* 固定间隔档位，上限 10 */
    /**
     * 审核Step。
     */
    private int reviewStep;

    /**
     * 下一次复习的间隔，单位为秒。
     */
    private long intervalSeconds;

    /* 计划复习时间；首次复习为 null */
    /**
     * 计划调度时间。
     */
    private LocalDateTime scheduledAt;

    /**
     * 复习时间。
     */
    private LocalDateTime reviewedAt;

    /**
     * 下一次计划复习时间。
     */
    private LocalDateTime nextReviewAt;

    /* NEW / EARLY / ON_TIME / OVERDUE */
    /**
     * 调度状态。
     */
    private String timingStatus;
}

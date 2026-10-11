/*
 * ReviewResultVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.learning.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核结果响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultVO {
    /**
     * 任务标识。
     */
    private String taskId;
    /**
     * 卡片标识。
     */
    private String cardId;
    /**
     * 评分。
     */
    private String rating;
    /**
     * 上一项间隔Days。
     */
    private int previousIntervalDays;
    /**
     * 下一项间隔Days。
     */
    private int nextIntervalDays;
    /**
     * 下一次计划复习时间。
     */
    private LocalDateTime nextReviewAt;
    /**
     * systemSuggestedLevel。
     */
    private String systemSuggestedLevel;
}

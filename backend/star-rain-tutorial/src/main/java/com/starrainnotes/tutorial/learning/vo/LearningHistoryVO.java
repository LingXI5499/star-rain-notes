/*
 * LearningHistoryVO：教程与学习模块的响应数据文件。
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
 * 学习历史响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 事件类型。
     */
    private String eventType;
    /**
     * 教程标识。
     */
    private String tutorialId;
    /**
     * 章节标识。
     */
    private String chapterId;
    /**
     * 卡片标识。
     */
    private String cardId;
    /**
     * 学习任务标识。
     */
    private String studyTaskId;
    /**
     * 审核任务标识。
     */
    private String reviewTaskId;
    /**
     * occurred时间。
     */
    private LocalDateTime occurredAt;
}

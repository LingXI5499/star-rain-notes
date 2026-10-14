/*
 * LearningProgressVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.learning.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习进度响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressVO {
    private Long rowVersion;
    /**
     * 教程标识。
     */
    private String tutorialId;
    /**
     * 章节标识。
     */
    private String chapterId;
    /**
     * 教程固定链接标识。
     */
    private String tutorialSlug;
    /**
     * 章节固定链接标识。
     */
    private String chapterSlug;
    /**
     * 章节标题。
     */
    private String chapterTitle;
    /**
     * scrollAnchor。
     */
    private String scrollAnchor;
    /**
     * 进度比例。
     */
    private BigDecimal progressRatio;
    /**
     * 学习秒总数。
     */
    private Long studySecondsTotal;
    /**
     * 已完成时间。
     */
    private LocalDateTime completedAt;
    /**
     * 最近Studied时间。
     */
    private LocalDateTime lastStudiedAt;
}

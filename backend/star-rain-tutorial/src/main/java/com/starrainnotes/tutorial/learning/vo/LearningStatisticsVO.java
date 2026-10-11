/*
 * LearningStatisticsVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.learning.vo;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习Statistics响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatisticsVO {
    /**
     * started章节列表。
     */
    private long startedChapters;
    /**
     * 已完成章节列表。
     */
    private long completedChapters;
    /**
     * 学习秒总数。
     */
    private long studySecondsTotal;
    /**
     * 有效Plans。
     */
    private long activePlans;
    /**
     * today学习Tasks。
     */
    private long todayStudyTasks;
    /**
     * dueReviews。
     */
    private long dueReviews;
    /**
     * 已完成Reviews。
     */
    private long completedReviews;
    /**
     * 掌握Distribution。
     */
    private Map<String, Long> masteryDistribution;
}

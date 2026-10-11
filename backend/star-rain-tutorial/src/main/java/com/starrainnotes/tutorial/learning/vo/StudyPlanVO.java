/*
 * StudyPlanVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.learning.vo;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习计划响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 教程标识。
     */
    private String tutorialId;
    /**
     * 名称。
     */
    private String name;
    /**
     * scope类型。
     */
    private String scopeType;
    /**
     * scope标识。
     */
    private String scopeId;
    /**
     * 开始日期。
     */
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    private LocalDate endDate;
    /**
     * 学习Weekdays。
     */
    private List<Integer> studyWeekdays;
    /**
     * daily目标分钟。
     */
    private Integer dailyTargetMinutes;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * generated版本。
     */
    private Integer generatedVersion;
}

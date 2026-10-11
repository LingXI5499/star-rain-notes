/*
 * StudyPlanEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 个人学习计划实体。表名与列清单由 mapper/tutorial/StudyPlanMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解；「null 不覆盖已有值」的字段策略写在 XML 的 updateDefinition 里。
 */
/**
 * 学习计划实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class StudyPlanEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 教程标识。
     */
    private Long tutorialId;
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
    private Long scopeId;
    /**
     * 开始日期。
     */
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    private LocalDate endDate;
    /**
     * 学习WeekdaysJSON。
     */
    private String studyWeekdaysJson;
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
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

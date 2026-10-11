package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 个人学习计划实体。表名与列清单由 mapper/tutorial/StudyPlanMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解；「null 不覆盖已有值」的字段策略写在 XML 的 updateDefinition 里。
 */
@Data
public class StudyPlanEntity {
    private Long id;
    private Long accountId;
    private Long tutorialId;
    private String name;
    private String scopeType;
    private Long scopeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String studyWeekdaysJson;
    private Integer dailyTargetMinutes;
    private String status;
    private Integer generatedVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

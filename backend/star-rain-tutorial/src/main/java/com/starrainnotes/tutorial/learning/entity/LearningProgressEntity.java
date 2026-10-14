/*
 * LearningProgressEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 章节学习进度实体。表名与列清单由 mapper/tutorial/LearningProgressMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 学习进度实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class LearningProgressEntity {
    private Long rowVersion;
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
     * 分组标识。
     */
    private Long groupId;
    /**
     * 章节标识。
     */
    private Long chapterId;
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
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

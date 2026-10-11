/*
 * ReviewScheduleEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 知识卡片复习计划实体。表名与列清单由 mapper/tutorial/ReviewScheduleMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 审核Schedule实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class ReviewScheduleEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 知识卡片标识。
     */
    private Long knowledgeCardId;
    /**
     * step索引。
     */
    private Integer stepIndex;
    /**
     * 当前间隔Days。
     */
    private Integer currentIntervalDays;
    /**
     * 下一次计划复习时间。
     */
    private LocalDateTime nextReviewAt;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 最近结果时间。
     */
    private LocalDateTime lastResultAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

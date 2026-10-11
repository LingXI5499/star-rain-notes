/*
 * ReviewResultEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 单次复习结果实体。表名与列清单由 mapper/tutorial/ReviewResultMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 审核结果实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class ReviewResultEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 审核任务标识。
     */
    private Long reviewTaskId;
    /**
     * schedule标识。
     */
    private Long scheduleId;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 知识卡片标识。
     */
    private Long knowledgeCardId;
    /**
     * recall评分。
     */
    private String recallRating;
    /**
     * 上一项间隔Days。
     */
    private Integer previousIntervalDays;
    /**
     * 下一项间隔Days。
     */
    private Integer nextIntervalDays;
    /**
     * 上一项下一项审核时间。
     */
    private LocalDateTime previousNextReviewAt;
    /**
     * calculated下一项审核时间。
     */
    private LocalDateTime calculatedNextReviewAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}

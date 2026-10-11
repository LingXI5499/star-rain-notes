/*
 * LearningHistoryEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 个人学习事件历史实体。表名与列清单由 mapper/tutorial/LearningHistoryMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 学习历史实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class LearningHistoryEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 事件类型。
     */
    private String eventType;
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
     * 知识卡片标识。
     */
    private Long knowledgeCardId;
    /**
     * 学习任务标识。
     */
    private Long studyTaskId;
    /**
     * 审核任务标识。
     */
    private Long reviewTaskId;
    /**
     * 详情JSON。
     */
    private String detailJson;
    /**
     * occurred时间。
     */
    private LocalDateTime occurredAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}

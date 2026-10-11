/*
 * UserQuestionAnswerEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 用户章节问题答案实体。表名与列清单由 mapper/tutorial/UserQuestionAnswerMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 用户问题答案实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class UserQuestionAnswerEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 问题标识。
     */
    private Long questionId;
    /**
     * 用户提交的回答文本。
     */
    private String answerText;
    /**
     * 首次提交时间。
     */
    private LocalDateTime firstSubmittedAt;
    /**
     * 引用Unlocked时间。
     */
    private LocalDateTime referenceUnlockedAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

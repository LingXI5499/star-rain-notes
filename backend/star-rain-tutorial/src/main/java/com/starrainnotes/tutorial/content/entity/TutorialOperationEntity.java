/*
 * TutorialOperationEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import lombok.Data;

/** Frozen operation proposal; business data is changed only after approval. */
@Data
public class TutorialOperationEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 操作。
     */
    private String operation;
    /**
     * argumentsJSON。
     */
    private String argumentsJson;
    /**
     * baselineJSON。
     */
    private String baselineJson;
    /**
     * scope键。
     */
    private String scopeKey;
    /**
     * 待处理Scope。
     */
    private String pendingScope;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * 申请人账号标识。
     */
    private Long applicantAccountId;
    /**
     * 执行留言。
     */
    private String executionMessage;
}

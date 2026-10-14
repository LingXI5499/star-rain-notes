/*
 * TutorialRevisionEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 教程不可变版本快照实体。表名与列清单由 mapper/tutorial/TutorialRevisionMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 教程修订实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class TutorialRevisionEntity {
    private String summaryJson;
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 教程标识。
     */
    private Long tutorialId;
    /**
     * 修订不含。
     */
    private Integer revisionNo;
    /**
     * 版本或修订的引用值。
     */
    private String revisionRef;
    /**
     * 快照JSON。
     */
    private String snapshotJson;
    /**
     * 内容摘要256。
     */
    private String contentSha256;
    /**
     * 修订原因。
     */
    private String revisionReason;
    /**
     * 创建该记录的账号标识。
     */
    private Long createdByAccountId;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}

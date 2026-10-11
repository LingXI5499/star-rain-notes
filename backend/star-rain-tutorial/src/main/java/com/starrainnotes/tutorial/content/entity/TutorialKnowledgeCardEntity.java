/*
 * TutorialKnowledgeCardEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 章节知识卡片实体。表名与列清单由 mapper/tutorial/TutorialKnowledgeCardMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 教程知识卡片实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class TutorialKnowledgeCardEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     * 初始值为 <code>0L</code>。
     */
    private Long rowVersion = 0L;
    /**
     * 章节标识。
     */
    private Long chapterId;
    /**
     * 内容版本。
     * 初始值为 <code>1</code>。
     */
    private Integer contentVersion = 1;
    /**
     * 知识卡片正面的提示文本。
     */
    private String frontText;
    /**
     * 知识卡片背面的 Markdown 内容。
     */
    private String backMarkdown;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

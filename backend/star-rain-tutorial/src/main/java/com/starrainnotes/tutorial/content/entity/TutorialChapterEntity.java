/*
 * TutorialChapterEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 章节实体。表名与列清单由 mapper/tutorial/TutorialChapterMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 *
 * summary 原先靠 @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证
 * 「为 null 也写进 UPDATE」（清空摘要），这个语义已由 XML 的 updateContent 语句显式承担。
 */
/**
 * 教程章节实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class TutorialChapterEntity {
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
     * 教程标识。
     */
    private Long tutorialId;
    /**
     * 分组标识。
     */
    private Long groupId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
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

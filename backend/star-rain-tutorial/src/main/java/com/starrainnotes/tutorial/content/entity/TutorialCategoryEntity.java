/*
 * TutorialCategoryEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 知识体系分类实体。表名与列清单由 mapper/tutorial/TutorialCategoryMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
/**
 * 教程分类实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class TutorialCategoryEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 名称。
     */
    private String name;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 教程数量。
     */
    private Long tutorialCount;
    /**
     * 章节数量。
     */
    private Long chapterCount;
}

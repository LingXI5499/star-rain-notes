/*
 * TutorialCategoryVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.content.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教程分类响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialCategoryVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
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
     * 教程数量。
     */
    private Long tutorialCount;
    /**
     * 章节数量。
     */
    private Long chapterCount;
}

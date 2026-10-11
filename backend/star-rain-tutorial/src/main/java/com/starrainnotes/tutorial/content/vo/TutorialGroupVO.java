/*
 * TutorialGroupVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.content.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教程分组响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialGroupVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 教程标识。
     */
    private String tutorialId;
    /**
     * 标题。
     */
    private String title;
    /**
     * 描述。
     */
    private String description;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 章节数量。
     */
    private long chapterCount;
    /**
     * 章节列表。
     */
    private List<TutorialChapterVO> chapters;
}

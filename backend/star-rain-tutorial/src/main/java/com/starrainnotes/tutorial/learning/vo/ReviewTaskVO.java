/*
 * ReviewTaskVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.learning.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核任务响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 卡片标识。
     */
    private String cardId;
    /**
     * 章节标识。
     */
    private String chapterId;
    /**
     * 教程固定链接标识。
     */
    private String tutorialSlug;
    /**
     * 章节固定链接标识。
     */
    private String chapterSlug;
    /**
     * 知识卡片正面的提示文本。
     */
    private String frontText;
    /**
     * due时间。
     */
    private LocalDateTime dueAt;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
}

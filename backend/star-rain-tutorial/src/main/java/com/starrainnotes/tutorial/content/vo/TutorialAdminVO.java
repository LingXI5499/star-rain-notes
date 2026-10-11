/*
 * TutorialAdminVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.content.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教程后台响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialAdminVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 分类标识。
     */
    private String categoryId;
    /**
     * 分类名称。
     */
    private String categoryName;
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
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 发布状态。
     */
    private String publicationStatus;
    /**
     * editing状态。
     */
    private String editingStatus;
    /**
     * 章节数量。
     */
    private long chapterCount;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}

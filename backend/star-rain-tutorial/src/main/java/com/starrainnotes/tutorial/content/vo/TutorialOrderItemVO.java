/*
 * TutorialOrderItemVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.tutorial.content.vo;

import lombok.Data;

/** Lightweight complete category order, requested only in explicit sorting mode. */
@Data
public class TutorialOrderItemVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 标题。
     */
    private String title;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
}

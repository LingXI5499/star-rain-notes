/*
 * MasteryVO：教程与学习模块的响应数据文件。
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
 * 掌握响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryVO {
    /**
     * 卡片标识。
     */
    private String cardId;
    /**
     * 章节标识。
     */
    private String chapterId;
    /**
     * 知识卡片正面的提示文本。
     */
    private String frontText;
    /**
     * systemSuggestedLevel。
     */
    private String systemSuggestedLevel;
    /**
     * 用户SelfLevel。
     */
    private String userSelfLevel;
    /**
     * 最近Recall评分。
     */
    private String lastRecallRating;
    /**
     * 证据数量。
     */
    private Integer evidenceCount;
    /**
     * 首次学习时间。
     */
    private LocalDateTime firstLearnedAt;
    /**
     * 最近证据时间。
     */
    private LocalDateTime lastEvidenceAt;
}

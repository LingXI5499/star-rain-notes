/*
 * LearningAnswerVO：教程与学习模块的响应数据文件。
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
 * 学习答案响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAnswerVO {
    /**
     * 问题标识。
     */
    private String questionId;
    /**
     * 用户提交的回答文本。
     */
    private String answerText;
    /**
     * 首次提交时间。
     */
    private LocalDateTime firstSubmittedAt;
    /**
     * 引用Unlocked时间。
     */
    private LocalDateTime referenceUnlockedAt;
}

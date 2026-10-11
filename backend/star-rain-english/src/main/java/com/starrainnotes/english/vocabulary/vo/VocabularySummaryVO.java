/*
 * VocabularySummaryVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 英语首页的四格学习统计，字段与 V1 LearningSummary 对齐。
 *
 * 词汇域的口径：
 *   total        记忆计划总条数（sr_english_vocabulary_memory 的所有行）
 *   completed    计划内且已走完十档间隔（learning_status=ACTIVE 且 review_step>=10）
 *   inProgress   计划内但还没走完十档
 *   dueForReview 计划内且 next_review_at <= 现在
 */
/**
 * 词汇摘要响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularySummaryVO {

    /**
     * 已完成。
     */
    private long completed;

    /**
     * 内部进度。
     */
    private long inProgress;

    /**
     * due面向审核。
     */
    private long dueForReview;

    /**
     * 符合查询条件的总记录数。
     */
    private long total;
}

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularySummaryVO {

    private long completed;

    private long inProgress;

    private long dueForReview;

    private long total;
}

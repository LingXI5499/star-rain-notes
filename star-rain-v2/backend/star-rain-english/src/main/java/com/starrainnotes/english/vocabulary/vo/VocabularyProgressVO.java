package com.starrainnotes.english.vocabulary.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习进度汇总：只呈现「计划间隔」与「真实完成记录」，不伪装成记忆保持率。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyProgressVO {

    /* 计划内（ACTIVE）单词数 */
    private long activeWords;

    /* 当前到期数 */
    private long dueWords;

    /* 站点时区「今日」完成的复习数 */
    private long completedToday;

    /* 累计复习次数 */
    private long totalReviews;

    /* 累计记忆次数（memory_count 之和） */
    private long totalMemoryCount;

    private List<VocabularyReviewHistoryVO> recentReviews;
}

package com.starrainnotes.english.vocabulary.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 今日学习队列：到期复习 + 主题新词，按到期优先排序。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyQueueVO {

    private List<VocabularyStudyCardVO> items;

    /* 全量到期数（不受每日复习上限截断），用于入口卡与页头提示 */
    private int dueCount;

    /* 本次纳入队列的新词数，同样受每日新词上限约束 */
    private int newCount;

    private String generatedAt;
}

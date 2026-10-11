/*
 * VocabularyProgressVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习进度汇总：只呈现「计划间隔」与「真实完成记录」，不伪装成记忆保持率。
 */
/**
 * 词汇进度响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyProgressVO {

    /* 计划内（ACTIVE）单词数 */
    /**
     * 有效Words。
     */
    private long activeWords;

    /* 当前到期数 */
    /**
     * dueWords。
     */
    private long dueWords;

    /* 站点时区「今日」完成的复习数 */
    /**
     * 已完成Today。
     */
    private long completedToday;

    /* 累计复习次数 */
    /**
     * 总数Reviews。
     */
    private long totalReviews;

    /* 累计记忆次数（memory_count 之和） */
    /**
     * 总数记忆数量。
     */
    private long totalMemoryCount;

    /**
     * recentReviews。
     */
    private List<VocabularyReviewHistoryVO> recentReviews;
}

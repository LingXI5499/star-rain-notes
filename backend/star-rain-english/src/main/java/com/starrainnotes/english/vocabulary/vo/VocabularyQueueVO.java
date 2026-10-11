/*
 * VocabularyQueueVO：英语学习模块的响应数据文件。
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
 * 今日学习队列：到期复习 + 主题新词，按到期优先排序。
 */
/**
 * 词汇Queue响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyQueueVO {

    /**
     * 条目集合。
     */
    private List<VocabularyStudyCardVO> items;

    /* 全量到期数（不受每日复习上限截断），用于入口卡与页头提示 */
    /**
     * due数量。
     */
    private int dueCount;

    /* 本次纳入队列的新词数，同样受每日新词上限约束 */
    /**
     * new数量。
     */
    private int newCount;

    /**
     * generated时间。
     */
    private String generatedAt;
}

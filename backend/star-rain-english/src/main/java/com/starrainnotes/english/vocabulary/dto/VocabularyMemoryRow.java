/*
 * VocabularyMemoryRow：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 记忆状态行（仓储 -> 服务）。时间保持 UTC LocalDateTime，由服务统一转成站点时区的 ISO 串。
 */
/**
 * 词汇记忆行，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryRow {

    /**
     * 单词标识。
     */
    private Long wordId;

    /**
     * 记忆数量。
     */
    private int memoryCount;

    /**
     * 审核Step。
     */
    private int reviewStep;

    /**
     * 审核数量。
     */
    private int reviewCount;

    /**
     * 首次学习时间。
     */
    private LocalDateTime firstLearnedAt;

    /**
     * 最近一次复习时间。
     */
    private LocalDateTime lastReviewedAt;

    /**
     * 下一次计划复习时间。
     */
    private LocalDateTime nextReviewAt;

    /* 数据库列可能为空（历史行），服务侧按 NEW 处理 */
    /**
     * 学习状态。
     */
    private String learningStatus;

    /* 单卡显示覆盖；为空表示跟随全局 */
    /**
     * 显示Mode。
     */
    private String displayMode;
}

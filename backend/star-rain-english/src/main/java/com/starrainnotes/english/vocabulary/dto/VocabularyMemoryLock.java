/*
 * VocabularyMemoryLock：英语学习模块的请求与传输数据文件。
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
 * 复习前的状态快照（SELECT ... FOR UPDATE 的结果），用于计算下一次复习排期。
 */
/**
 * 词汇记忆锁，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryLock {

    /**
     * 记忆数量。
     */
    private int memoryCount;

    /**
     * 审核数量。
     */
    private int reviewCount;

    /* 计划复习时间；首次复习为空 */
    /**
     * 下一次计划复习时间。
     */
    private LocalDateTime nextReviewAt;
}

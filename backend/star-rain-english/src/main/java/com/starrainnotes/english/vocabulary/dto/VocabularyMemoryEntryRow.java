/*
 * VocabularyMemoryEntryRow：英语学习模块的请求与传输数据文件。
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
 * 记忆集合行：只带「是否已加入计划」需要的字段。
 */
/**
 * 词汇记忆入口行，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryEntryRow {

    /**
     * 单词标识。
     */
    private Long wordId;

    /**
     * 记忆数量。
     */
    private int memoryCount;

    /**
     * 最近记忆时间。
     */
    private LocalDateTime lastMemoryAt;
}

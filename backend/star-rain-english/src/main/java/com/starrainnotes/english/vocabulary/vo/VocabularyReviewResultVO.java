/*
 * VocabularyReviewResultVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次复习写入的结果：新状态 + 本次复习序号/间隔/时机判定。
 *
 * duplicate 为 true 表示同一个 reviewSessionId 被重放（网络重试、双击），
 * 服务端没有重复计数，前端应据此提示「已计入」而不是再写一次。
 */
/**
 * 词汇审核结果响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewResultVO {

    /**
     * 记忆。
     */
    private VocabularyMemoryVO memory;

    /**
     * 审核请求的业务编号。
     */
    private int reviewNumber;

    /**
     * 下一次复习的间隔，单位为秒。
     */
    private long intervalSeconds;

    /**
     * 调度状态。
     */
    private String timingStatus;

    /**
     * duplicate。
     */
    private boolean duplicate;
}

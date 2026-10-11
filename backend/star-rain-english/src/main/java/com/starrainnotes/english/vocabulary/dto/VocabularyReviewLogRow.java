/*
 * VocabularyReviewLogRow：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 已有复习日志行的回放结果。同一个 review_session_id 被重放时直接返回这一行，
 * 服务端不再自增记忆次数。
 */
/**
 * 词汇审核Log行，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewLogRow {

    /**
     * 单词标识。
     */
    private long wordId;

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
}

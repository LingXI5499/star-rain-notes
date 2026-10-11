/*
 * VocabularyMemoryVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 单词个人记忆状态。
 *
 * learningStatus 为 NEW 时代表「词条存在但未加入记忆计划」，与后端的缺省视图一致，
 * 前端不需要为「没有进度」单独造一份对象。
 */
/**
 * 词汇记忆响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryVO {

    /**
     * 单词标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
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

    /* 站点时区的 ISO 偏移时间串，与其它模块的时间输出口径一致 */
    /**
     * 首次学习时间。
     */
    private String firstLearnedAt;

    /**
     * 最近一次复习时间。
     */
    private String lastReviewedAt;

    /**
     * 下一次计划复习时间。
     */
    private String nextReviewAt;

    /**
     * 学习状态。
     */
    private String learningStatus;

    /* 单卡显示覆盖；null 表示跟随全局设置 */
    /**
     * 显示Mode。
     */
    private String displayMode;
}

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    private int memoryCount;

    private int reviewStep;

    private int reviewCount;

    /* 站点时区的 ISO 偏移时间串，与其它模块的时间输出口径一致 */
    private String firstLearnedAt;

    private String lastReviewedAt;

    private String nextReviewAt;

    private String learningStatus;

    /* 单卡显示覆盖；null 表示跟随全局设置 */
    private String displayMode;
}

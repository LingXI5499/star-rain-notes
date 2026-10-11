/*
 * VocabularyStudySettingsVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.english.vocabulary.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习设置视图（全局词卡显示、复习方向、每日上限）。
 */
/**
 * 词汇学习Settings响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyStudySettingsVO {

    /**
     * show英语。
     */
    private boolean showEnglish;

    /**
     * showChinese。
     */
    private boolean showChinese;

    /**
     * 审核Direction。
     */
    private String reviewDirection;

    /**
     * dailyNew上限。
     */
    private int dailyNewLimit;

    /**
     * daily审核上限。
     */
    private int dailyReviewLimit;
}

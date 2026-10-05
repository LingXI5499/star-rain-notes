package com.starrainnotes.english.vocabulary.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习设置视图（全局词卡显示、复习方向、每日上限）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyStudySettingsVO {

    private boolean showEnglish;

    private boolean showChinese;

    private String reviewDirection;

    private int dailyNewLimit;

    private int dailyReviewLimit;
}

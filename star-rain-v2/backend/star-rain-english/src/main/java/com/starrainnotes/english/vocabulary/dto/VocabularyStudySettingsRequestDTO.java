package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 学习设置写入请求。reviewDirection 取值与 V1 完全一致：EN_TO_ZH / ZH_TO_EN / MIXED。
 */
@Data
public class VocabularyStudySettingsRequestDTO {

    private boolean showEnglish;

    private boolean showChinese;

    @NotBlank
    @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|MIXED", message = "复习方向只能是英译中、中译英或随机混合")
    private String reviewDirection;

    @Min(0)
    @Max(200)
    private int dailyNewLimit;

    @Min(1)
    @Max(1000)
    private int dailyReviewLimit;
}

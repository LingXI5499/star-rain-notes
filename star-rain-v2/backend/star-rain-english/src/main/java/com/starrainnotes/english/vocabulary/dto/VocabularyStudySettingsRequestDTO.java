package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 学习设置写入请求。MIXED 仅用于旧客户端兼容，写入时映射为 EN_TO_ZH。
 */
@Data
public class VocabularyStudySettingsRequestDTO {

    private boolean showEnglish;

    private boolean showChinese;

    @NotBlank
    @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH|MIXED", message = "复习方向只能是英译中、中译英或听音辨词")
    private String reviewDirection;

    @Min(0)
    @Max(200)
    private int dailyNewLimit;

    @Min(1)
    @Max(1000)
    private int dailyReviewLimit;
}

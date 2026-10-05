package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 单词复习完成请求。
 *
 * reviewSessionId 由前端生成（UUID），是幂等键：同一个会话重放只记一次复习。
 */
@Data
public class VocabularyReviewRequestDTO {

    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F\\-]{36}", message = "复习会话标识必须是 UUID")
    private String reviewSessionId;

    @NotBlank
    @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN", message = "复习方向只能是英译中或中译英")
    private String direction;
}

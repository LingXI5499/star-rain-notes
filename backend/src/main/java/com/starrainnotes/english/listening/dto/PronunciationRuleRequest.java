package com.starrainnotes.english.listening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PronunciationRuleRequest(
        @NotBlank @Size(max = 30) String ruleType,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 150) String slug,
        @NotBlank @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown,
        Long audioMediaId,
        Integer sortOrder) {
}

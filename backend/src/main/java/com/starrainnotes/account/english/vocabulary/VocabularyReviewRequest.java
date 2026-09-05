package com.starrainnotes.account.english.vocabulary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VocabularyReviewRequest(
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
        String reviewSessionId,
        @NotBlank @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN") String direction) {
}

package com.starrainnotes.account.english.vocabulary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VocabularyDisplayRequest(
        @NotBlank @Pattern(regexp = "BILINGUAL|ENGLISH_ONLY|CHINESE_ONLY") String displayMode) {
}

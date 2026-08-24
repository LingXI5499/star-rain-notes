package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GrammarLessonRequest(
        @NotNull Long sectionId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[0-9]+-[0-9]+$", message = "slug must use a stable numeric lesson number, for example 2-3")
        String slug,
        @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown) {
}

package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GrammarLessonRequest(
        @NotNull Long sectionId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 50)
        @Pattern(regexp = "^\\s*$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown) {
}

package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGrammarCourseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 300) String subtitle,
        @Size(max = 1000) String summary,
        String introduction,
        String roadmapMarkdown,
        Long coverMediaId,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription) {
}

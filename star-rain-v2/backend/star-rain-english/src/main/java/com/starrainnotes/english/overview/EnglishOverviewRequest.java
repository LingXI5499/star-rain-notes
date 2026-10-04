package com.starrainnotes.english.overview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnglishOverviewRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String subtitle,
        @Size(max = 10000) String introduction,
        @Size(max = 200000) String roadmapMarkdown) {
}

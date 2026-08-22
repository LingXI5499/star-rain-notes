package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId} request body.
 * publishStatus / publishedAt can never be changed through a plain update.
 */
public record UpdateChapterRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown) {
}

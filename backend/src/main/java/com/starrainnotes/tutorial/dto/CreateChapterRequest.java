package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/tutorials/{tutorialId}/chapters request body.
 * publishStatus / publishedAt are set only by /publish and /withdraw.
 */
public record CreateChapterRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        Long parentId,
        @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown) {
}

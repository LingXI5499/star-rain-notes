package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/tutorials/{tutorialId} request body.
 * publishStatus / publishedAt are intentionally absent — a plain PUT can never
 * change the publish lifecycle (enforced by the DTO and fail-on-unknown).
 */
public record UpdateTutorialRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 150)
        @Pattern(regexp = "^\\s*$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @NotBlank @Size(max = 1000) String summary,
        Long coverMediaId,
        Integer sortOrder,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription) {
}

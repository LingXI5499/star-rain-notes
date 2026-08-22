package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/tutorials request body.
 * publishStatus / publishedAt are intentionally absent — lifecycle changes
 * only through /publish and /withdraw actions.
 */
public record CreateTutorialRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @NotBlank @Size(max = 1000) String summary,
        Long coverMediaId,
        Integer sortOrder,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription) {
}

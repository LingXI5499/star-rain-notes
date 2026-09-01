package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/tutorial-categories request body.
 */
public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100)
        @Pattern(regexp = "^\\s*$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        Integer sortOrder) {
}

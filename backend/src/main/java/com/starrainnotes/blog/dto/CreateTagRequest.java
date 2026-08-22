package com.starrainnotes.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/blog/tags request body.
 */
public record CreateTagRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 60)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug) {
}

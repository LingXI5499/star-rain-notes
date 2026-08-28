package com.starrainnotes.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/blog/tags/{tagId} request body.
 */
public record UpdateTagRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 60)
        @Pattern(regexp = "^\\s*$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug) {
}

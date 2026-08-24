package com.starrainnotes.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/v1/admin/blog/posts/{postId} request body.
 * publishStatus / publishedAt can never be changed through a plain update;
 * tag relations are replaced transactionally (delete old, insert new).
 */
public record UpdatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @NotBlank @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown,
        Long coverMediaId,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription,
        List<Long> tagIds,
        List<@NotBlank @Size(max = 50) String> tagNames) {
}

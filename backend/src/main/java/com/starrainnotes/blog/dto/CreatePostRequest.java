package com.starrainnotes.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/v1/admin/blog/posts request body.
 * publishStatus / publishedAt are absent — lifecycle changes only through
 * /publish and /withdraw actions. Tag relations are committed in the same
 * transaction as the post.
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 150)
        @Pattern(regexp = "^\\s*$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @NotBlank @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown,
        Long coverMediaId,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription,
        List<Long> tagIds,
        List<@NotBlank @Size(max = 50) String> tagNames) {
}

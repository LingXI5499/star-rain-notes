package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * PUT /api/v1/admin/portfolio/projects/{projectId} request body.
 * publishStatus / publishedAt can never be changed through a plain update.
 */
public record UpdateProjectRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase kebab-case")
        String slug,
        @NotBlank @Size(max = 1000) String summary,
        @Size(max = 200) String role,
        List<String> techStack,
        @NotBlank String bodyMarkdown,
        Long coverMediaId,
        @Size(max = 500) String repositoryUrl,
        @Size(max = 500) String demoUrl,
        @Pattern(regexp = "^(DEVELOPING|COMPLETED|ONLINE)$", message = "projectStatus must be one of DEVELOPING, COMPLETED, ONLINE")
        String projectStatus,
        Boolean featured,
        Integer sortOrder,
        LocalDate startedAt,
        LocalDate completedAt,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription) {
}

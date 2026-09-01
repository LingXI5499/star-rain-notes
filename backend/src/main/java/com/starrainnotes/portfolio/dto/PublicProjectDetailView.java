package com.starrainnotes.portfolio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Public portfolio case-study detail with adjacent published projects.
 */
public record PublicProjectDetailView(
        Long id,
        String title,
        String slug,
        String summary,
        String role,
        List<String> techStack,
        String bodyMarkdown,
        String coverUrl,
        String repositoryUrl,
        String demoUrl,
        String projectStatus,
        LocalDate startedAt,
        LocalDate completedAt,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String updatedAt,
        PrevNextProjectView previous,
        PrevNextProjectView next) {
}

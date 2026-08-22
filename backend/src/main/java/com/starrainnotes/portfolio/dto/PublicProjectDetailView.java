package com.starrainnotes.portfolio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Public portfolio case-study detail. No Prev/Next (04 §12).
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
        String updatedAt) {
}

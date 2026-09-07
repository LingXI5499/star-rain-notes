package com.starrainnotes.portfolio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin portfolio project detail (entity never exposed).
 */
public record AdminProjectDetailView(
        Long id,
        String title,
        String slug,
        String summary,
        String role,
        List<String> techStack,
        String bodyMarkdown,
        Long coverMediaId,
        String coverUrl,
        Integer coverWidth,
        Integer coverHeight,
        String coverSrcSet,
        String repositoryUrl,
        String demoUrl,
        String publishStatus,
        String projectStatus,
        boolean featured,
        Integer sortOrder,
        LocalDate startedAt,
        LocalDate completedAt,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String createdAt,
        String updatedAt,
        List<ProjectMediaView> gallery) {
}

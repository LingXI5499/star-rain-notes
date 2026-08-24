package com.starrainnotes.tutorial.dto;

/**
 * Chapter detail (only this endpoint returns bodyMarkdown).
 */
public record ChapterDetailView(
        Long id,
        Long tutorialId,
        Long groupId,
        String nodeType,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        String publishStatus,
        Integer sortOrder,
        String publishedAt,
        String updatedAt) {
}

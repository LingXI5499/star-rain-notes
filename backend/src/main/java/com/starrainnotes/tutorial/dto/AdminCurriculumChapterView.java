package com.starrainnotes.tutorial.dto;

/** Lightweight chapter row for the two-column curriculum manager. */
public record AdminCurriculumChapterView(
        Long id,
        Long groupId,
        String title,
        String slug,
        String publishStatus,
        Integer sortOrder,
        String updatedAt) {
}

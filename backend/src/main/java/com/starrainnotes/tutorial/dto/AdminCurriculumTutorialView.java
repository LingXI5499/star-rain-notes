package com.starrainnotes.tutorial.dto;

/** Tutorial context returned with the curriculum manager payload. */
public record AdminCurriculumTutorialView(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        String slug,
        String publishStatus) {
}

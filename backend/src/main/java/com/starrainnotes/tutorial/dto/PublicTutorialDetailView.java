package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Public tutorial detail — metadata, category path, published chapter count,
 * first chapter and the curriculum tree. Never contains draft/withdrawn nodes.
 */
public record PublicTutorialDetailView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        List<CategoryPathView> categoryPath,
        long publishedChapterCount,
        FirstChapterView firstChapter,
        List<CurriculumNodeView> curriculum,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String updatedAt) {
}

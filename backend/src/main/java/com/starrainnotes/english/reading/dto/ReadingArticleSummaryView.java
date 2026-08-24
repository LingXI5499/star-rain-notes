package com.starrainnotes.english.reading.dto;

import java.util.List;

/**
 * Admin/public reading-article list item. Never carries the Markdown body.
 */
public record ReadingArticleSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        Integer readingLevel,
        String cefrLevel,
        Integer wordCount,
        Integer estimatedMinutes,
        String publishStatus,
        boolean hasExercises,
        String updatedAt,
        List<ReadingTagRef> tags) {

    public ReadingArticleSummaryView withTags(List<ReadingTagRef> tags) {
        return new ReadingArticleSummaryView(id, title, slug, summary, coverUrl, readingLevel, cefrLevel,
                wordCount, estimatedMinutes, publishStatus, hasExercises, updatedAt, tags);
    }

    public ReadingArticleSummaryView withExercises(boolean hasExercises) {
        return new ReadingArticleSummaryView(id, title, slug, summary, coverUrl, readingLevel, cefrLevel,
                wordCount, estimatedMinutes, publishStatus, hasExercises, updatedAt, tags);
    }
}

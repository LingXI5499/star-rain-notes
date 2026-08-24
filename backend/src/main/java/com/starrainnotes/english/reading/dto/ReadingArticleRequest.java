package com.starrainnotes.english.reading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create/update payload for a reading article. Stats are never accepted from
 * the client — they are recomputed from {@code bodyMarkdown} by the backend.
 * Tag ids are validated against the shared taxonomy dimensions (TOPIC/GENRE/
 * ABILITY) at the service layer.
 */
public record ReadingArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150) String slug,
        @NotBlank @Size(max = 1000) String summary,
        @NotBlank String bodyMarkdown,
        Long coverMediaId,
        @NotNull Integer readingLevel,
        @NotBlank @Size(max = 2) String cefrLevel,
        @Size(max = 200) String sourceName,
        @Size(max = 500) String sourceUrl,
        @Size(max = 500) String copyrightNote,
        Integer sortOrder,
        List<Long> topicTagIds,
        List<Long> genreTagIds,
        List<Long> abilityTagIds,
        List<Long> grammarLessonIds) {
}

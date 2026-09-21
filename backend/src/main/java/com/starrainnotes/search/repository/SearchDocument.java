package com.starrainnotes.search.repository;

import java.time.LocalDateTime;

/** Internal SQL projection; ranking and HTTP response assembly remain in SearchService. */
public record SearchDocument(
        String type, Long id, String title, String summary, String body, String slug,
        String tutorialSlug, String chapterSlug, LocalDateTime activityAt) {
}

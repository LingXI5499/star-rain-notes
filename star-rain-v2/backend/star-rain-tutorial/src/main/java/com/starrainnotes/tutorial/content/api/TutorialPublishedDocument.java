package com.starrainnotes.tutorial.content.api;

import java.time.LocalDateTime;
import java.util.List;

public record TutorialPublishedDocument(Long id, String slug, String title, String summary,
                                        LocalDateTime publishedAt, List<TutorialChapterDocument> chapters) {
}

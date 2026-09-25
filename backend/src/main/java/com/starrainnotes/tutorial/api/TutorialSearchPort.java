package com.starrainnotes.tutorial.api;

import java.time.LocalDateTime;
import java.util.List;

public interface TutorialSearchPort {
    record TutorialHit(long id, String title, String summary, String slug, LocalDateTime updatedAt) {
    }

    record ChapterHit(long id, String title, String summary, String body, String tutorialSlug, String chapterSlug,
                      LocalDateTime updatedAt) {
    }

    List<TutorialHit> tutorials(String pattern);

    List<ChapterHit> chapters(String pattern);
}

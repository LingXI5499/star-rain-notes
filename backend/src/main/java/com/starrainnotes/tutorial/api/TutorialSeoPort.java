package com.starrainnotes.tutorial.api;

import java.time.LocalDateTime;
import java.util.List;

public interface TutorialSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record State(String slug, boolean published) {
    }

    record TutorialPage(String title, String summary, LocalDateTime publishedAt, LocalDateTime updatedAt,
                        Long coverMediaId, String categoryName) {
    }

    record ChapterPage(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt,
                       String tutorialTitle) {
    }

    record ChapterPath(String tutorialSlug, String chapterSlug, LocalDateTime updatedAt) {
    }

    State visibility(long id);

    List<Card> publishedTutorials();

    TutorialPage publishedTutorial(String slug);

    List<Card> publishedChapters(String tutorialSlug);

    ChapterPage publishedChapter(String tutorialSlug, String chapterSlug);

    List<ChapterPath> publishedChapterPaths();
}

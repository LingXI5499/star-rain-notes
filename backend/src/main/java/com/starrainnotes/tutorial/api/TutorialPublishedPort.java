package com.starrainnotes.tutorial.api;

import java.time.LocalDateTime;
import java.util.List;

public interface TutorialPublishedPort {
    record Chapter(long id, String title, String tutorialSlug, String chapterSlug, LocalDateTime updatedAt) {
    }

    List<Chapter> latestPublishedChapters(int limit);
}

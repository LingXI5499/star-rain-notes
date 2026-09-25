package com.starrainnotes.english.writing.api;

import java.time.LocalDateTime;
import java.util.List;

public interface WritingSearchPort {
    record ResourceHit(long id, String title, String summary, String body, String slug, LocalDateTime updatedAt) {
    }

    record PromptHit(long id, String title, String summary, String background, String requirements, String slug,
                     LocalDateTime updatedAt) {
    }

    List<ResourceHit> resources(String pattern);

    List<PromptHit> prompts(String pattern);
}

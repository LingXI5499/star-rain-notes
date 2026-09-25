package com.starrainnotes.english.writing.api;

import java.time.LocalDateTime;
import java.util.List;

public interface WritingSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Resource(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt, Long coverMediaId) {
    }

    record Prompt(String title, String summary, String background, String requirements, LocalDateTime publishedAt,
                  LocalDateTime updatedAt, Long coverMediaId) {
    }

    List<Card> publishedResources();

    List<Card> publishedPrompts();

    Resource publishedResource(String slug);

    Prompt publishedPrompt(String slug);
}

package com.starrainnotes.english.reading.api;

import java.time.LocalDateTime;
import java.util.List;

public interface ReadingSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Article(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt, Long coverMediaId) {
    }

    List<Card> published();

    Article publishedArticle(String slug);
}

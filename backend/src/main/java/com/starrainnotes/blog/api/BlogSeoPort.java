package com.starrainnotes.blog.api;

import java.time.LocalDateTime;
import java.util.List;

public interface BlogSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Article(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt, Long coverMediaId) {
    }

    record State(String slug, boolean published) {
    }

    State visibility(long id);

    List<Card> published();

    Article publishedArticle(String slug);
}

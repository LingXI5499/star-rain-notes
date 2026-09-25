package com.starrainnotes.english.grammar.api;

import java.time.LocalDateTime;
import java.util.List;

public interface GrammarSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Article(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt) {
    }

    List<Card> published();

    Article publishedArticle(String slug);
}

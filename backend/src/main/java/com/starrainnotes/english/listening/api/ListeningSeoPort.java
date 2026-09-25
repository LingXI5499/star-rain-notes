package com.starrainnotes.english.listening.api;

import java.time.LocalDateTime;
import java.util.List;

public interface ListeningSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Material(String title, String summary, String transcript, LocalDateTime publishedAt, LocalDateTime updatedAt, Long coverMediaId) {
    }

    record Rule(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt) {
    }

    List<Card> publishedMaterials();

    List<Card> publishedRules();

    Material publishedMaterial(String slug);

    Rule publishedRule(String slug);
}

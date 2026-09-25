package com.starrainnotes.english.shared.bundle.api;

import java.time.LocalDateTime;
import java.util.List;

public interface BundleSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Page(String title, String summary, LocalDateTime publishedAt, LocalDateTime updatedAt) {
    }

    List<Card> published();

    Page publishedBundle(String slug);
}

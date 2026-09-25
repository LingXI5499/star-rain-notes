package com.starrainnotes.english.reading.api;

import java.time.LocalDateTime;
import java.util.List;

public interface ReadingSearchPort {
    record Hit(long id, String title, String summary, String body, String slug, LocalDateTime updatedAt) {
    }

    List<Hit> search(String pattern);
}

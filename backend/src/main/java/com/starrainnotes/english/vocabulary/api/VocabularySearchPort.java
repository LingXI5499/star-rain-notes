package com.starrainnotes.english.vocabulary.api;

import java.time.LocalDateTime;
import java.util.List;

public interface VocabularySearchPort {
    record Hit(long id, String word, String translation, String inflections, long themeId, String themeName,
               LocalDateTime updatedAt) {
    }

    List<Hit> search(String pattern);
}

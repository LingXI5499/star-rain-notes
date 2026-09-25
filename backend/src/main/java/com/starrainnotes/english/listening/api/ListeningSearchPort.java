package com.starrainnotes.english.listening.api;

import java.time.LocalDateTime;
import java.util.List;

public interface ListeningSearchPort {
    record Hit(long id, String title, String summary, String body, String slug, LocalDateTime updatedAt) {
    }

    List<Hit> materials(String pattern);

    List<Hit> pronunciationRules(String pattern);
}

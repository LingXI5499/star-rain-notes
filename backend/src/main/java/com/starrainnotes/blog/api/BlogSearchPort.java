package com.starrainnotes.blog.api;

import java.time.LocalDateTime;
import java.util.List;

public interface BlogSearchPort {
    record Hit(long id, String title, String summary, String body, String slug, LocalDateTime publishedAt) {
    }

    List<Hit> search(String pattern);
}

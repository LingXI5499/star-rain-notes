package com.starrainnotes.blog.api;

import java.time.LocalDateTime;
import java.util.List;

public interface BlogPublishedPort {
    record Post(long id, String title, String slug, LocalDateTime publishedAt) {
    }

    List<Post> latestPublished(int limit);
}

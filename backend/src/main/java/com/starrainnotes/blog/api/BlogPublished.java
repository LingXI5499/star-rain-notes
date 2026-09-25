package com.starrainnotes.blog.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlogPublished implements BlogPublishedPort {
    private final JdbcTemplate jdbc;

    public BlogPublished(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Post> latestPublished(int limit) {
        return jdbc.query("""
                SELECT id, title, slug, published_at
                FROM blog_post
                WHERE publish_status = 'PUBLISHED'
                ORDER BY published_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new Post(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getTimestamp("published_at").toLocalDateTime()), limit);
    }
}

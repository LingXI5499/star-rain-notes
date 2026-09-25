package com.starrainnotes.blog.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlogSearch implements BlogSearchPort {
    private final JdbcTemplate jdbc;

    public BlogSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hit> search(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, published_at FROM blog_post
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), rs.getTimestamp("published_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }
}

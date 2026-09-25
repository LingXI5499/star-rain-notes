package com.starrainnotes.english.listening.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListeningSearch implements ListeningSearchPort {
    private final JdbcTemplate jdbc;

    public ListeningSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hit> materials(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, transcript_markdown, slug, updated_at FROM english_listening_item
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR transcript_markdown LIKE ?)
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("transcript_markdown"), rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    @Override
    public List<Hit> pronunciationRules(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM english_listening_pronunciation_rule
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }
}

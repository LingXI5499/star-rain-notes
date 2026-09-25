package com.starrainnotes.english.grammar.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GrammarSearch implements GrammarSearchPort {
    private final JdbcTemplate jdbc;

    public GrammarSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hit> search(String pattern) {
        return jdbc.query("""
                SELECT l.id, l.title, l.summary, l.body_markdown, l.slug, l.updated_at
                FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id = l.course_id
                WHERE c.publish_status = 'PUBLISHED' AND l.publish_status = 'PUBLISHED'
                  AND (l.title LIKE ? OR l.summary LIKE ? OR l.body_markdown LIKE ?)
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }
}

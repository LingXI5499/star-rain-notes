package com.starrainnotes.english.writing.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WritingSearch implements WritingSearchPort {
    private final JdbcTemplate jdbc;

    public WritingSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ResourceHit> resources(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM english_writing_resource
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> new ResourceHit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    @Override
    public List<PromptHit> prompts(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, background_markdown, requirements_markdown, slug, updated_at
                FROM english_writing_prompt
                WHERE publish_status = 'PUBLISHED'
                  AND (title LIKE ? OR summary LIKE ? OR background_markdown LIKE ? OR requirements_markdown LIKE ?)
                """, (rs, rowNum) -> new PromptHit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("background_markdown"), rs.getString("requirements_markdown"), rs.getString("slug"),
                rs.getTimestamp("updated_at").toLocalDateTime()), pattern, pattern, pattern, pattern);
    }
}

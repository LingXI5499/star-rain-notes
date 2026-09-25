package com.starrainnotes.tutorial.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TutorialSearch implements TutorialSearchPort {
    private final JdbcTemplate jdbc;

    public TutorialSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TutorialHit> tutorials(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, slug, updated_at FROM tutorial
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ?)
                """, (rs, rowNum) -> new TutorialHit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()), pattern, pattern);
    }

    @Override
    public List<ChapterHit> chapters(String pattern) {
        return jdbc.query("""
                SELECT n.id, n.title, n.summary, n.body_markdown, n.slug AS chapter_slug, t.slug AS tutorial_slug, n.updated_at
                FROM tutorial_node n JOIN tutorial t ON t.id = n.tutorial_id
                WHERE t.publish_status = 'PUBLISHED' AND n.node_type = 'CHAPTER' AND n.publish_status = 'PUBLISHED'
                  AND (n.title LIKE ? OR n.summary LIKE ? OR n.body_markdown LIKE ?)
                """, (rs, rowNum) -> new ChapterHit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("tutorial_slug"), rs.getString("chapter_slug"),
                rs.getTimestamp("updated_at").toLocalDateTime()), pattern, pattern, pattern);
    }
}

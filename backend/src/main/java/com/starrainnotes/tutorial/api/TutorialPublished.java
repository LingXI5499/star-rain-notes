package com.starrainnotes.tutorial.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TutorialPublished implements TutorialPublishedPort {
    private final JdbcTemplate jdbc;

    public TutorialPublished(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Chapter> latestPublishedChapters(int limit) {
        return jdbc.query("""
                SELECT n.id, n.title, n.slug AS chapter_slug, n.updated_at,
                       t.slug AS tutorial_slug
                FROM tutorial_node n
                JOIN tutorial t ON t.id = n.tutorial_id
                WHERE t.publish_status = 'PUBLISHED'
                  AND n.node_type = 'CHAPTER'
                  AND n.publish_status = 'PUBLISHED'
                ORDER BY n.updated_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new Chapter(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("tutorial_slug"),
                rs.getString("chapter_slug"),
                rs.getTimestamp("updated_at").toLocalDateTime()), limit);
    }
}

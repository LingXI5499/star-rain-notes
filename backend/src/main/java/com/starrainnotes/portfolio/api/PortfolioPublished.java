package com.starrainnotes.portfolio.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioPublished implements PortfolioPublishedPort {
    private final JdbcTemplate jdbc;

    public PortfolioPublished(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Project> latestPublished(int limit) {
        return jdbc.query("""
                SELECT id, title, slug, updated_at
                FROM portfolio_project
                WHERE publish_status = 'PUBLISHED'
                ORDER BY updated_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new Project(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getTimestamp("updated_at").toLocalDateTime()), limit);
    }

    @Override
    public List<Featured> featured(int limit) {
        return jdbc.query("""
                SELECT p.id, p.title, p.slug, p.summary, p.project_status, p.cover_media_id
                FROM portfolio_project p
                WHERE p.publish_status = 'PUBLISHED'
                ORDER BY p.featured DESC,
                         CASE WHEN p.featured = 1 THEN p.sort_order ELSE 0 END ASC,
                         CASE WHEN p.featured = 0 THEN p.updated_at END DESC,
                         p.id DESC
                LIMIT ?
                """, (rs, rowNum) -> {
            long cover = rs.getLong("cover_media_id");
            Long coverMediaId = rs.wasNull() ? null : cover;
            return new Featured(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("slug"),
                    rs.getString("summary"),
                    coverMediaId,
                    rs.getString("project_status"));
        }, limit);
    }
}

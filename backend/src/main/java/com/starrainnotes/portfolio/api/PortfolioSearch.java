package com.starrainnotes.portfolio.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioSearch implements PortfolioSearchPort {
    private final JdbcTemplate jdbc;

    public PortfolioSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hit> search(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM portfolio_project
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }
}

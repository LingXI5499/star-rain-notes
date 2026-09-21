package com.starrainnotes.site.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** JDBC data access for the bounded administrative dashboard read model. */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardCounts counts() {
        return new DashboardCounts(
                count("SELECT COUNT(*) FROM tutorial"),
                count("SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER'"),
                count("SELECT COUNT(*) FROM blog_post"),
                count("SELECT COUNT(*) FROM portfolio_project"),
                count("SELECT COUNT(*) FROM tutorial WHERE publish_status = 'DRAFT'"),
                count("SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER' AND publish_status = 'DRAFT'"),
                count("SELECT COUNT(*) FROM blog_post WHERE publish_status = 'DRAFT'"),
                count("SELECT COUNT(*) FROM portfolio_project WHERE publish_status = 'DRAFT'"));
    }

    public List<RecentContentRow> recentContent() {
        return jdbc.query("""
                SELECT 'TUTORIAL' AS type, id, title, publish_status, updated_at FROM tutorial
                UNION ALL
                SELECT 'CHAPTER', id, title, COALESCE(publish_status, ''), updated_at
                FROM tutorial_node WHERE node_type = 'CHAPTER'
                UNION ALL
                SELECT 'BLOG', id, title, publish_status, updated_at FROM blog_post
                UNION ALL
                SELECT 'PORTFOLIO', id, title, publish_status, updated_at FROM portfolio_project
                ORDER BY updated_at DESC
                LIMIT 10
                """, (resultSet, rowNum) -> new RecentContentRow(
                resultSet.getString("type"), resultSet.getLong("id"), resultSet.getString("title"),
                resultSet.getString("publish_status"), resultSet.getTimestamp("updated_at").toLocalDateTime()));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    public record DashboardCounts(long tutorials, long chapters, long blogPosts, long portfolioProjects,
                                  long draftTutorials, long draftChapters, long draftBlogPosts, long draftPortfolioProjects) {
    }

    public record RecentContentRow(String type, Long id, String title, String publishStatus, LocalDateTime updatedAt) {
    }
}

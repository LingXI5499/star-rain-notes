package com.starrainnotes.site.service;

import com.starrainnotes.site.dto.ContentCountsView;
import com.starrainnotes.site.dto.DashboardView;
import com.starrainnotes.site.dto.DraftCountsView;
import com.starrainnotes.site.dto.RecentContentView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin dashboard (04 §9): content counts, draft counts and recent content.
 * Deliberately no analytics / PV / UV / charts.
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public DashboardService(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public DashboardView getDashboard() {
        long tutorials = count("SELECT COUNT(*) FROM tutorial");
        long chapters = count("SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER'");
        long blogPosts = count("SELECT COUNT(*) FROM blog_post");
        long portfolioProjects = count("SELECT COUNT(*) FROM portfolio_project");

        long draftTutorials = count("SELECT COUNT(*) FROM tutorial WHERE publish_status = 'DRAFT'");
        long draftChapters = count(
                "SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER' AND publish_status = 'DRAFT'");
        long draftBlogPosts = count("SELECT COUNT(*) FROM blog_post WHERE publish_status = 'DRAFT'");
        long draftPortfolio = count("SELECT COUNT(*) FROM portfolio_project WHERE publish_status = 'DRAFT'");

        List<RecentContentView> recent = jdbc.query("""
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
                """, (rs, rowNum) -> new RecentContentView(
                rs.getString("type"),
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("publish_status"),
                formatUtc(rs.getTimestamp("updated_at"))));

        return new DashboardView(
                new ContentCountsView(tutorials, chapters, blogPosts, portfolioProjects),
                new DraftCountsView(draftTutorials, draftChapters, draftBlogPosts, draftPortfolio),
                recent);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private String formatUtc(Timestamp utcTimestamp) {
        LocalDateTime utc = utcTimestamp.toLocalDateTime();
        return ZonedDateTime.of(utc, ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(timezone.get()))
                .format(ISO_OFFSET);
    }
}

package com.starrainnotes.seo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SeoSitemapRepository {
    private final JdbcTemplate jdbc;

    public SeoSitemapRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SitemapEntry> publishedEntries() {
        List<SitemapEntry> entries = new ArrayList<>();
        add(entries, "SELECT CONCAT('/tutorials/',slug),updated_at FROM tutorial WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/tutorials/',t.slug,'/',n.slug),n.updated_at FROM tutorial_node n JOIN tutorial t ON t.id=n.tutorial_id WHERE n.node_type='CHAPTER' AND n.publish_status='PUBLISHED' AND t.publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/blog/',slug),updated_at FROM blog_post WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/portfolio/',slug),updated_at FROM portfolio_project WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/grammar/',l.slug),l.updated_at FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id=l.course_id WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/reading/',slug),updated_at FROM english_reading_article WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/listening/',slug),updated_at FROM english_listening_item WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/listening/pronunciation/',slug),updated_at FROM english_listening_pronunciation_rule WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/writing/resources/',slug),updated_at FROM english_writing_resource WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/writing/practice/',slug),updated_at FROM english_writing_prompt WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/bundles/',slug),updated_at FROM english_learning_bundle WHERE publish_status='PUBLISHED'");
        return entries;
    }

    private void add(List<SitemapEntry> entries, String sql) {
        jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                entries.add(new SitemapEntry(resultSet.getString(1), time(resultSet.getTimestamp(2)))));
    }

    private LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record SitemapEntry(String path, LocalDateTime updatedAt) {
    }
}

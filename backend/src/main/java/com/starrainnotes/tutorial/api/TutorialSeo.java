package com.starrainnotes.tutorial.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TutorialSeo implements TutorialSeoPort {
    private final JdbcTemplate jdbc;

    public TutorialSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public State visibility(long id) {
        return jdbc.query("SELECT slug,publish_status FROM tutorial WHERE id=?",
                rs -> rs.next() ? new State(rs.getString(1), "PUBLISHED".equals(rs.getString(2))) : null, id);
    }

    @Override
    public List<Card> publishedTutorials() {
        return jdbc.query("SELECT t.title,t.slug,t.summary,t.updated_at FROM tutorial t WHERE t.publish_status='PUBLISHED' ORDER BY t.sort_order,t.id",
                this::cards);
    }

    @Override
    public TutorialPage publishedTutorial(String slug) {
        return jdbc.query("""
                SELECT t.title,t.summary,t.published_at,t.updated_at,t.cover_media_id,c.name category_name
                FROM tutorial t JOIN tutorial_category c ON c.id=t.category_id
                WHERE t.slug=? AND t.publish_status='PUBLISHED'
                """, rs -> rs.next() ? new TutorialPage(rs.getString("title"), rs.getString("summary"), time(rs, "published_at"),
                time(rs, "updated_at"), cover(rs), rs.getString("category_name")) : null, slug);
    }

    @Override
    public List<Card> publishedChapters(String tutorialSlug) {
        return jdbc.query("""
                SELECT title,slug,summary,updated_at FROM tutorial_node
                WHERE tutorial_id=(SELECT id FROM tutorial WHERE slug=?) AND node_type='CHAPTER' AND publish_status='PUBLISHED'
                ORDER BY sort_order,id
                """, this::cards, tutorialSlug);
    }

    @Override
    public ChapterPage publishedChapter(String tutorialSlug, String chapterSlug) {
        return jdbc.query("""
                SELECT n.title,n.summary,n.body_markdown,n.published_at,n.updated_at,t.title tutorial_title
                FROM tutorial_node n JOIN tutorial t ON t.id=n.tutorial_id
                WHERE t.slug=? AND t.publish_status='PUBLISHED' AND n.slug=? AND n.node_type='CHAPTER' AND n.publish_status='PUBLISHED'
                """, rs -> rs.next() ? new ChapterPage(rs.getString("title"), rs.getString("summary"), rs.getString("body_markdown"),
                time(rs, "published_at"), time(rs, "updated_at"), rs.getString("tutorial_title")) : null, tutorialSlug, chapterSlug);
    }

    @Override
    public List<ChapterPath> publishedChapterPaths() {
        return jdbc.query("""
                SELECT t.slug,n.slug,n.updated_at FROM tutorial_node n JOIN tutorial t ON t.id=n.tutorial_id
                WHERE n.node_type='CHAPTER' AND n.publish_status='PUBLISHED' AND t.publish_status='PUBLISHED'
                """, (rs, row) -> new ChapterPath(rs.getString(1), rs.getString(2), time(rs, 3)));
    }

    private List<Card> cards(ResultSet rs) throws SQLException {
        List<Card> result = new java.util.ArrayList<>();
        while (rs.next()) result.add(new Card(rs.getString(1), rs.getString(2), rs.getString(3), time(rs, 4)));
        return result;
    }

    private Long cover(ResultSet rs) throws SQLException {
        long value = rs.getLong("cover_media_id");
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return time(rs, rs.findColumn(column));
    }

    private LocalDateTime time(ResultSet rs, int column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}

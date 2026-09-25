package com.starrainnotes.blog.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BlogSeo implements BlogSeoPort {
    private final JdbcTemplate jdbc;

    public BlogSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public State visibility(long id) {
        return jdbc.query("SELECT slug,publish_status FROM blog_post WHERE id=?",
                rs -> rs.next() ? new State(rs.getString(1), "PUBLISHED".equals(rs.getString(2))) : null, id);
    }

    @Override
    public List<Card> published() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM blog_post WHERE publish_status='PUBLISHED' ORDER BY published_at DESC,id DESC",
                this::cards);
    }

    @Override
    public Article publishedArticle(String slug) {
        return jdbc.query("""
                SELECT title,summary,body_markdown,published_at,updated_at,cover_media_id FROM blog_post
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> rs.next() ? article(rs) : null, slug);
    }

    private Article article(ResultSet rs) throws SQLException {
        long cover = rs.getLong("cover_media_id");
        Long coverId = rs.wasNull() ? null : cover;
        return new Article(rs.getString("title"), rs.getString("summary"), rs.getString("body_markdown"),
                time(rs.getTimestamp("published_at")), time(rs.getTimestamp("updated_at")), coverId);
    }

    private List<Card> cards(ResultSet rs) throws SQLException {
        List<Card> result = new ArrayList<>();
        while (rs.next()) result.add(new Card(rs.getString(1), rs.getString(2), rs.getString(3), time(rs.getTimestamp(4))));
        return result;
    }

    private LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}

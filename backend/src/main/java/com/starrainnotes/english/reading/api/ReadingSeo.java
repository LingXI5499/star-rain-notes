package com.starrainnotes.english.reading.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReadingSeo implements ReadingSeoPort {
    private final JdbcTemplate jdbc;

    public ReadingSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Card> published() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_reading_article WHERE publish_status='PUBLISHED' ORDER BY sort_order,id",
                this::cards);
    }

    @Override
    public Article publishedArticle(String slug) {
        return jdbc.query("""
                SELECT title,summary,body_markdown,published_at,updated_at,cover_media_id FROM english_reading_article
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> {
            if (!rs.next()) return null;
            long cover = rs.getLong("cover_media_id");
            Long coverId = rs.wasNull() ? null : cover;
            return new Article(rs.getString("title"), rs.getString("summary"), rs.getString("body_markdown"),
                    time(rs.getTimestamp("published_at")), time(rs.getTimestamp("updated_at")), coverId);
        }, slug);
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

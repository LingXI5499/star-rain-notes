package com.starrainnotes.english.listening.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ListeningSeo implements ListeningSeoPort {
    private final JdbcTemplate jdbc;

    public ListeningSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Card> publishedMaterials() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_listening_item WHERE publish_status='PUBLISHED' ORDER BY sort_order,id", this::cards);
    }

    @Override
    public List<Card> publishedRules() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_listening_pronunciation_rule WHERE publish_status='PUBLISHED' ORDER BY sort_order,id", this::cards);
    }

    @Override
    public Material publishedMaterial(String slug) {
        return jdbc.query("""
                SELECT title,summary,transcript_markdown,published_at,updated_at,cover_media_id FROM english_listening_item
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> {
            if (!rs.next()) return null;
            long cover = rs.getLong("cover_media_id");
            Long coverId = rs.wasNull() ? null : cover;
            return new Material(rs.getString(1), rs.getString(2), rs.getString(3), time(rs.getTimestamp(4)), time(rs.getTimestamp(5)), coverId);
        }, slug);
    }

    @Override
    public Rule publishedRule(String slug) {
        return jdbc.query("""
                SELECT title,summary,body_markdown,published_at,updated_at FROM english_listening_pronunciation_rule
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> rs.next() ? new Rule(rs.getString(1), rs.getString(2), rs.getString(3), time(rs.getTimestamp(4)), time(rs.getTimestamp(5))) : null, slug);
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

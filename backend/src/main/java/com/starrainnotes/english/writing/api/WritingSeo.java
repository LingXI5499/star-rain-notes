package com.starrainnotes.english.writing.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class WritingSeo implements WritingSeoPort {
    private final JdbcTemplate jdbc;

    public WritingSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Card> publishedResources() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_writing_resource WHERE publish_status='PUBLISHED' ORDER BY sort_order,id", this::cards);
    }

    @Override
    public List<Card> publishedPrompts() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_writing_prompt WHERE publish_status='PUBLISHED' ORDER BY sort_order,id", this::cards);
    }

    @Override
    public Resource publishedResource(String slug) {
        return jdbc.query("""
                SELECT title,summary,body_markdown,published_at,updated_at,cover_media_id FROM english_writing_resource
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> {
            if (!rs.next()) return null;
            long cover = rs.getLong("cover_media_id");
            Long coverId = rs.wasNull() ? null : cover;
            return new Resource(rs.getString(1), rs.getString(2), rs.getString(3), time(rs.getTimestamp(4)), time(rs.getTimestamp(5)), coverId);
        }, slug);
    }

    @Override
    public Prompt publishedPrompt(String slug) {
        return jdbc.query("""
                SELECT title,summary,background_markdown,requirements_markdown,published_at,updated_at,cover_media_id
                FROM english_writing_prompt WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> {
            if (!rs.next()) return null;
            long cover = rs.getLong("cover_media_id");
            Long coverId = rs.wasNull() ? null : cover;
            return new Prompt(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    time(rs.getTimestamp(5)), time(rs.getTimestamp(6)), coverId);
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

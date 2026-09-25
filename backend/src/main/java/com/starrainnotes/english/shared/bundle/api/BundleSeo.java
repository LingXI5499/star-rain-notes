package com.starrainnotes.english.shared.bundle.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BundleSeo implements BundleSeoPort {
    private final JdbcTemplate jdbc;

    public BundleSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Card> published() {
        return jdbc.query("SELECT title,slug,summary,updated_at FROM english_learning_bundle WHERE publish_status='PUBLISHED' ORDER BY sort_order,id", this::cards);
    }

    @Override
    public Page publishedBundle(String slug) {
        return jdbc.query("SELECT title,summary,published_at,updated_at FROM english_learning_bundle WHERE slug=? AND publish_status='PUBLISHED'",
                rs -> rs.next() ? new Page(rs.getString(1), rs.getString(2), time(rs.getTimestamp(3)), time(rs.getTimestamp(4))) : null, slug);
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

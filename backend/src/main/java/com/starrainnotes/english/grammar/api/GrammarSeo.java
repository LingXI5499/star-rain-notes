package com.starrainnotes.english.grammar.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GrammarSeo implements GrammarSeoPort {
    private final JdbcTemplate jdbc;

    public GrammarSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Card> published() {
        return jdbc.query("""
                SELECT l.title,l.slug,l.summary,l.updated_at FROM english_grammar_lesson l
                JOIN english_grammar_course c ON c.id=l.course_id
                WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED' ORDER BY l.sort_order,l.id
                """, this::cards);
    }

    @Override
    public Article publishedArticle(String slug) {
        return jdbc.query("""
                SELECT title,summary,body_markdown,published_at,updated_at FROM english_grammar_lesson
                WHERE slug=? AND publish_status='PUBLISHED'
                """, rs -> rs.next() ? new Article(rs.getString(1), rs.getString(2), rs.getString(3),
                time(rs.getTimestamp(4)), time(rs.getTimestamp(5))) : null, slug);
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

package com.starrainnotes.english.vocabulary.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VocabularySearch implements VocabularySearchPort {
    private final JdbcTemplate jdbc;

    public VocabularySearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hit> search(String pattern) {
        return jdbc.query("""
                SELECT w.id, w.word, w.translation, w.inflections, w.theme_id, t.name AS theme_name, w.updated_at
                FROM vocabulary_word w JOIN vocabulary_theme t ON t.id = w.theme_id
                WHERE w.word LIKE ? OR w.translation LIKE ? OR IFNULL(w.inflections, '') LIKE ?
                """, (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getString("word"), rs.getString("translation"),
                rs.getString("inflections"), rs.getLong("theme_id"), rs.getString("theme_name"),
                rs.getTimestamp("updated_at").toLocalDateTime()), pattern, pattern, pattern);
    }
}

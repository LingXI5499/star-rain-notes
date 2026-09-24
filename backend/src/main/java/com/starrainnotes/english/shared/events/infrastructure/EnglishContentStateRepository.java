package com.starrainnotes.english.shared.events.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Published-state lookup shared by content events and review capability. */
@Repository
public class EnglishContentStateRepository {
    private final JdbcTemplate jdbc;

    public EnglishContentStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ContentState state(EnglishContentKind kind, long id) {
        String table = switch (kind) {
            case READING -> "english_reading_article";
            case LISTENING -> "english_listening_item";
            case GRAMMAR_LESSON -> "english_grammar_lesson";
            case WRITING_PROMPT -> "english_writing_prompt";
            case WRITING_RESOURCE -> "english_writing_resource";
            case PRONUNCIATION_RULE -> "english_listening_pronunciation_rule";
            case BUNDLE -> "english_learning_bundle";
        };
        return jdbc.query("SELECT slug,publish_status FROM " + table + " WHERE id=?",
                rs -> rs.next()
                        ? new ContentState(rs.getString(1), "PUBLISHED".equals(rs.getString(2)))
                        : null, id);
    }

    public record ContentState(String slug, boolean published) { }
}

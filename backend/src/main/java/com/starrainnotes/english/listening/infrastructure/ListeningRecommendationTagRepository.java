package com.starrainnotes.english.listening.infrastructure;

import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.english.shared.content.TagMatchCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/** Recommendation tag reads owned by the listening module. */
@Repository
public class ListeningRecommendationTagRepository {
    private final JdbcTemplate jdbc;

    public ListeningRecommendationTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> tagIds(long contentId) {
        return jdbc.queryForList(
                "SELECT term_id FROM english_listening_item_tag WHERE listening_item_id=?",
                Long.class, contentId);
    }

    public List<TagMatchCandidate> matches(List<Long> termIds) {
        if (termIds.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(termIds.size(), "?"));
        return jdbc.query("""
                SELECT a.id,a.slug,a.title,a.cefr_level,COUNT(DISTINCT t.term_id) matches
                FROM english_listening_item a
                JOIN english_listening_item_tag t ON t.listening_item_id=a.id
                WHERE a.publish_status='PUBLISHED' AND t.term_id IN (
                """ + placeholders + """
                )
                GROUP BY a.id,a.slug,a.title,a.cefr_level
                ORDER BY matches DESC,a.id
                """, (rs, row) -> new TagMatchCandidate(
                new ContentDescriptor(EnglishContentType.LISTENING, rs.getLong("id"),
                        rs.getString("slug"), rs.getString("title"), null,
                        rs.getString("cefr_level"), null, "PUBLISHED", 0),
                rs.getInt("matches")), termIds.toArray());
    }
}

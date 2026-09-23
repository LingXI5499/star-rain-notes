package com.starrainnotes.english.reading.infrastructure;

import com.starrainnotes.english.reading.dto.ReadingGrammarRef;
import com.starrainnotes.english.reading.dto.ReadingTagRef;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reading tag and grammar relation persistence. */
@Repository
public class ReadingRelationRepository {
    private final JdbcTemplate jdbc;
    public ReadingRelationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean hasEnabledDimension(Long articleId, String dimension) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_reading_article_tag at
                JOIN english_taxonomy_term t ON t.id=at.term_id
                WHERE at.article_id=? AND t.dimension=? AND t.enabled=1
                """, Integer.class, articleId, dimension);
        return count != null && count > 0;
    }
    public void clear(Long articleId) {
        jdbc.update("DELETE FROM english_reading_article_tag WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM english_reading_article_grammar_lesson WHERE article_id=?", articleId);
    }
    public void addTag(Long articleId, Long termId, String role) {
        jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,?,?)",
                articleId, termId, role);
    }
    public void addGrammarLesson(Long articleId, Long lessonId) {
        jdbc.update("INSERT INTO english_reading_article_grammar_lesson(article_id,lesson_id) VALUES (?,?)",
                articleId, lessonId);
    }
    public List<String> enabledTagDimensions(Long termId) {
        return jdbc.query("SELECT dimension FROM english_taxonomy_term WHERE id=? AND enabled=1",
                (rs, row) -> rs.getString("dimension"), termId);
    }
    public boolean grammarLessonExists(Long lessonId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE id=?",
                Integer.class, lessonId);
        return count != null && count > 0;
    }

    public List<ReadingTagRef> findRootTagsByDimension(String dimension) {
        return jdbc.query("""
                SELECT id,name,slug,dimension,'TAG' AS tag_role FROM english_taxonomy_term
                WHERE dimension=? AND enabled=1 AND parent_id IS NULL ORDER BY sort_order,id
                """, (rs, row) -> new ReadingTagRef(rs.getLong("id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("dimension"), rs.getString("tag_role")), dimension);
    }

    public Map<Long, List<ReadingTagRef>> findTagsByArticleIds(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        String ids = String.join(",", articleIds.stream().map(String::valueOf).toList());
        Map<Long, List<ReadingTagRef>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT at.article_id, t.id, t.name, t.slug, t.dimension, at.tag_role
                FROM english_reading_article_tag at
                JOIN english_taxonomy_term t ON t.id=at.term_id
                WHERE at.article_id IN (""" + ids + ") ORDER BY t.dimension, t.sort_order, t.id",
                rs -> {
                    while (rs.next()) {
                        result.computeIfAbsent(rs.getLong("article_id"), k -> new ArrayList<>())
                                .add(new ReadingTagRef(rs.getLong("id"), rs.getString("name"), rs.getString("slug"),
                                        rs.getString("dimension"), rs.getString("tag_role")));
                    }
                    return null;
                });
        return result;
    }

    public List<ReadingGrammarRef> findGrammarRefs(Long articleId) {
        return jdbc.query("""
                SELECT gl.id, gl.title, gl.slug FROM english_reading_article_grammar_lesson agl
                JOIN english_grammar_lesson gl ON gl.id=agl.lesson_id
                WHERE agl.article_id=? ORDER BY gl.sort_order, gl.id
                """, (rs, row) -> new ReadingGrammarRef(rs.getLong("id"), rs.getString("title"),
                rs.getString("slug")), articleId);
    }
}

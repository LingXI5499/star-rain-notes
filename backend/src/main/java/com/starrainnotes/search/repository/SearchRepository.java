package com.starrainnotes.search.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** Parameter-bound SQL projections for published global-search documents. */
@Repository
public class SearchRepository {

    private final JdbcTemplate jdbc;

    public SearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SearchDocument> tutorials(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, slug, updated_at FROM tutorial
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ?)
                """, (rs, rowNum) -> document("TUTORIAL", rs.getLong("id"), rs.getString("title"),
                rs.getString("summary"), null, rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern);
    }

    public List<SearchDocument> chapters(String pattern) {
        return jdbc.query("""
                SELECT n.id, n.title, n.summary, n.body_markdown, n.slug AS chapter_slug, t.slug AS tutorial_slug, n.updated_at
                FROM tutorial_node n JOIN tutorial t ON t.id = n.tutorial_id
                WHERE t.publish_status = 'PUBLISHED' AND n.node_type = 'CHAPTER' AND n.publish_status = 'PUBLISHED'
                  AND (n.title LIKE ? OR n.summary LIKE ? OR n.body_markdown LIKE ?)
                """, (rs, rowNum) -> document("CHAPTER", rs.getLong("id"), rs.getString("title"),
                rs.getString("summary"), rs.getString("body_markdown"), null, rs.getString("tutorial_slug"),
                rs.getString("chapter_slug"), rs.getTimestamp("updated_at").toLocalDateTime()), pattern, pattern, pattern);
    }

    public List<SearchDocument> blogs(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, published_at FROM blog_post
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> document("BLOG", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("published_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> portfolios(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM portfolio_project
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> document("PORTFOLIO", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> grammar(String pattern) {
        return jdbc.query("""
                SELECT l.id, l.title, l.summary, l.body_markdown, l.slug, l.updated_at
                FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id = l.course_id
                WHERE c.publish_status = 'PUBLISHED' AND l.publish_status = 'PUBLISHED'
                  AND (l.title LIKE ? OR l.summary LIKE ? OR l.body_markdown LIKE ?)
                """, (rs, rowNum) -> document("GRAMMAR", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> reading(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM english_reading_article
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> document("READING", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> listeningMaterials(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, transcript_markdown, slug, updated_at FROM english_listening_item
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR transcript_markdown LIKE ?)
                """, (rs, rowNum) -> document("LISTENING", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("transcript_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> pronunciationRules(String pattern) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM english_listening_pronunciation_rule
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> document("PRONUNCIATION", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), null, null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
    }

    public List<SearchDocument> writing(String pattern) {
        List<SearchDocument> documents = jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at FROM english_writing_resource
                WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> document("WRITING", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), "resource", null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern);
        documents.addAll(jdbc.query("""
                SELECT id, title, summary, CONCAT(background_markdown, '\n', requirements_markdown) AS body_markdown, slug, updated_at
                FROM english_writing_prompt
                WHERE publish_status = 'PUBLISHED'
                  AND (title LIKE ? OR summary LIKE ? OR background_markdown LIKE ? OR requirements_markdown LIKE ?)
                """, (rs, rowNum) -> document("WRITING", rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("slug"), "practice", null, rs.getTimestamp("updated_at").toLocalDateTime()),
                pattern, pattern, pattern, pattern));
        return documents;
    }

    public List<SearchDocument> vocabulary(String pattern) {
        return jdbc.query("""
                SELECT w.id, w.word, w.translation, w.inflections, w.theme_id, t.name AS theme_name, w.updated_at
                FROM vocabulary_word w JOIN vocabulary_theme t ON t.id = w.theme_id
                WHERE w.word LIKE ? OR w.translation LIKE ? OR IFNULL(w.inflections, '') LIKE ?
                """, (rs, rowNum) -> {
            String translation = rs.getString("translation");
            String themeName = rs.getString("theme_name");
            String summary = themeName == null || themeName.isBlank() ? translation : translation + " · " + themeName;
            return document("WORD", rs.getLong("id"), rs.getString("word"), summary, rs.getString("inflections"),
                    null, String.valueOf(rs.getLong("theme_id")), null, rs.getTimestamp("updated_at").toLocalDateTime());
        }, pattern, pattern, pattern);
    }

    private static SearchDocument document(String type, Long id, String title, String summary, String body, String slug,
                                           String tutorialSlug, String chapterSlug, LocalDateTime activityAt) {
        return new SearchDocument(type, id, title, summary, body, slug, tutorialSlug, chapterSlug, activityAt);
    }
}

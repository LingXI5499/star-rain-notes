package com.starrainnotes.english.reading.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
import com.starrainnotes.english.reading.dto.ReadingExerciseView;
import com.starrainnotes.english.reading.domain.ReadingContentPort;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reading exercise binding and projection persistence. */
@Repository
public class ReadingExerciseRepository {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final EnglishExerciseMapper exerciseMapper;
    private final EnglishExerciseSafety safety;
    private final ObjectMapper objectMapper;
    private final SiteSettingsTimezone timezone;
    private final ReadingContentPort articles;
    public ReadingExerciseRepository(JdbcTemplate jdbc, EnglishExerciseMapper exerciseMapper,
            EnglishExerciseSafety safety, ObjectMapper objectMapper, SiteSettingsTimezone timezone,
            ReadingContentPort articles) {
        this.jdbc = jdbc;
        this.exerciseMapper = exerciseMapper;
        this.safety = safety;
        this.objectMapper = objectMapper;
        this.timezone = timezone;
        this.articles = articles;
    }

    public void requireArticle(Long articleId) { articles.requireExists(articleId); }
    public void requirePublished(Long articleId) { articles.requirePublished(articleId); }
    public void insert(EnglishExercise exercise) { exerciseMapper.insert(exercise); }
    public void update(EnglishExercise exercise) { exerciseMapper.updateById(exercise); }
    public void delete(Long exerciseId) { exerciseMapper.deleteById(exerciseId); }
    public void deleteBinding(Long articleId, Long exerciseId) {
        jdbc.update("DELETE FROM english_reading_article_exercise WHERE article_id=? AND exercise_id=?",
                articleId, exerciseId);
    }
    public List<ReadingExerciseView> listByArticle(Long articleId) {
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? ORDER BY e.sort_order, e.id
                """, (rs, row) -> mapAdmin(rs, articleId), articleId);
    }

    public void move(Long articleId, Long exerciseId, int targetIndex) {
        requireBinding(articleId, exerciseId);
        List<Long> ids = new ArrayList<>(jdbc.queryForList("""
                SELECT e.id FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? ORDER BY e.sort_order, e.id
                """, Long.class, articleId));
        ids.remove(exerciseId);
        ids.add(Math.min(Math.max(targetIndex, 0), ids.size()), exerciseId);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_exercise SET sort_order=100000 WHERE id IN ("
                + joinIds(ids) + ")");
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_exercise SET sort_order=? WHERE id=?", (i + 1) * 10, ids.get(i));
        }
    }

    public List<ReadingExercisePublicView> publicListPublished(Long articleId) {
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.score_value,e.sort_order
                FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? AND e.publish_status='PUBLISHED'
                ORDER BY e.sort_order, e.id
                """, (rs, row) -> toPublic(rs), articleId);
    }

    private Map<String, Object> sanitize(long exerciseId, String questionType, JsonNode config) {
        return safety.sanitize(questionType, config, exerciseId);
    }

    private ReadingExercisePublicView toPublic(java.sql.ResultSet rs) throws java.sql.SQLException {
        JsonNode config = readConfig(rs.getString("config_json"));
        long id = rs.getLong("id");
        String questionType = rs.getString("question_type");
        return new ReadingExercisePublicView(id, questionType,
                rs.getString("prompt_markdown"), sanitize(id, questionType, config), rs.getInt("score_value"),
                rs.getInt("sort_order"));
    }

    private ReadingExerciseView mapAdmin(java.sql.ResultSet rs, Long articleId) throws java.sql.SQLException {
        JsonNode config = readConfig(rs.getString("config_json"));
        return new ReadingExerciseView(rs.getLong("id"), articleId, rs.getString("question_type"),
                rs.getString("prompt_markdown"), objectMapper.convertValue(config,
                new TypeReference<>() { }), rs.getString("explanation_markdown"), rs.getInt("score_value"),
                rs.getInt("sort_order"), rs.getString("publish_status"),
                format(rs.getTimestamp("updated_at")));
    }

    private JsonNode readConfig(String configJson) {
        try {
            return objectMapper.readTree(configJson == null ? "{}" : configJson);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    public ReadingExerciseView adminView(Long exerciseId, Long articleId) {
        return jdbc.queryForObject("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_exercise e WHERE e.id=?
                """, (rs, row) -> mapAdmin(rs, articleId), exerciseId);
    }

    public void bind(Long articleId, Long exerciseId) {
        jdbc.update("INSERT INTO english_reading_article_exercise(article_id,exercise_id) VALUES (?,?)",
                articleId, exerciseId);
    }

    public Long nextSort(Long articleId) {
        Integer max = jdbc.queryForObject("""
                SELECT COALESCE(MAX(e.sort_order),0) FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id WHERE ae.article_id=?
                """, Integer.class, articleId);
        return (long) ((max == null ? 0 : max) + 10);
    }

    public void requireBinding(Long articleId, Long exerciseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article_exercise WHERE article_id=? AND exercise_id=?",
                Integer.class, articleId, exerciseId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise is not bound to this article.");
        }
    }

    public EnglishExercise requireExercise(Long exerciseId) {
        EnglishExercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise does not exist.");
        }
        return exercise;
    }

    public EnglishExercise requirePublishedBinding(Long articleId, Long exerciseId) {
        List<Long> ids = jdbc.queryForList("""
                SELECT e.id FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? AND e.id=? AND e.module_type='READING'
                  AND e.publish_status='PUBLISHED'
                """, Long.class, articleId, exerciseId);
        if (ids.isEmpty()) {
            throw invalidAnswer("The submitted exercise is not a published exercise of this article.");
        }
        return requireExercise(ids.get(0));
    }

    private String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    private ApiException invalidAnswer(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_ANSWER_INVALID",
                "Invalid reading answer", detail);
    }

}

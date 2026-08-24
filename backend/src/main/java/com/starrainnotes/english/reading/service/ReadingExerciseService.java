package com.starrainnotes.english.reading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.dto.ReadingCheckAnswerRequest;
import com.starrainnotes.english.reading.dto.ReadingCheckItemView;
import com.starrainnotes.english.reading.dto.ReadingCheckResultView;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
import com.starrainnotes.english.reading.dto.ReadingExerciseRequest;
import com.starrainnotes.english.reading.dto.ReadingExerciseView;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reading exercises bound to a single article (方案 §6.3, §六, §9.3).
 *
 * <p>Each exercise belongs to exactly one article: the mapping table has a
 * composite PK {@code (article_id, exercise_id)} and the service rejects any
 * exercise whose {@code module_type} is not READING or that is already bound
 * elsewhere. Public reads are sanitized (never leak answers) and scoring is
 * done server-side against the stored answer.</p>
 */
@Service
public class ReadingExerciseService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String READING = "READING";

    private final JdbcTemplate jdbc;
    private final EnglishExerciseMapper exerciseMapper;
    private final EnglishExerciseService exerciseRules;
    private final ReadingArticleService articleService;
    private final ObjectMapper objectMapper;
    private final SiteSettingsTimezone timezone;

    public ReadingExerciseService(JdbcTemplate jdbc, EnglishExerciseMapper exerciseMapper,
                                  EnglishExerciseService exerciseRules, ReadingArticleService articleService,
                                  ObjectMapper objectMapper, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.exerciseMapper = exerciseMapper;
        this.exerciseRules = exerciseRules;
        this.articleService = articleService;
        this.objectMapper = objectMapper;
        this.timezone = timezone;
    }

    public List<ReadingExerciseView> listByArticle(Long articleId) {
        requireArticle(articleId);
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? ORDER BY e.sort_order, e.id
                """, (rs, row) -> mapAdmin(rs, articleId), articleId);
    }

    @Transactional
    public ReadingExerciseView create(Long articleId, ReadingExerciseRequest request) {
        requireArticle(articleId);
        exerciseRules.validateConfig(READING, request.questionType(), request.configJson());
        Long exerciseId = nextSort(articleId);
        EnglishExercise exercise = new EnglishExercise();
        exercise.setModuleType(READING);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        exercise.setSortOrder(exerciseId.intValue());
        exercise.setPublishStatus(request.publishStatus() == null ? "DRAFT" : request.publishStatus());
        exerciseMapper.insert(exercise);
        bind(articleId, exercise.getId());
        return adminView(exercise.getId(), articleId);
    }

    @Transactional
    public ReadingExerciseView update(Long articleId, Long exerciseId, ReadingExerciseRequest request) {
        requireArticle(articleId);
        requireBinding(articleId, exerciseId);
        exerciseRules.validateConfig(READING, request.questionType(), request.configJson());
        EnglishExercise exercise = requireExercise(exerciseId);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        if (request.publishStatus() != null) exercise.setPublishStatus(request.publishStatus());
        exerciseMapper.updateById(exercise);
        return adminView(exerciseId, articleId);
    }

    @Transactional
    public void move(Long articleId, Long exerciseId, int targetIndex) {
        requireBinding(articleId, exerciseId);
        List<Long> ids = new ArrayList<>(jdbc.queryForList("""
                SELECT e.id FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? ORDER BY e.sort_order, e.id
                """, Long.class, articleId));
        ids.remove(exerciseId);
        ids.add(Math.min(targetIndex, ids.size()), exerciseId);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_exercise SET sort_order=100000 WHERE id IN ("
                + joinIds(ids) + ")");
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_exercise SET sort_order=? WHERE id=?", (i + 1) * 10, ids.get(i));
        }
    }

    @Transactional
    public void delete(Long articleId, Long exerciseId) {
        requireBinding(articleId, exerciseId);
        jdbc.update("DELETE FROM english_reading_article_exercise WHERE article_id=? AND exercise_id=?",
                articleId, exerciseId);
        exerciseMapper.deleteById(exerciseId);
    }

    @Transactional
    public void setPublishStatus(Long articleId, Long exerciseId, boolean published) {
        requireBinding(articleId, exerciseId);
        EnglishExercise exercise = requireExercise(exerciseId);
        exercise.setPublishStatus(published ? "PUBLISHED" : "DRAFT");
        exerciseMapper.updateById(exercise);
    }

    public List<ReadingExercisePublicView> publicListPublished(Long articleId) {
        requirePublished(articleId);
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.score_value,e.sort_order
                FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id
                WHERE ae.article_id=? AND e.publish_status='PUBLISHED'
                ORDER BY e.sort_order, e.id
                """, (rs, row) -> toPublic(rs), articleId);
    }

    @Transactional
    public ReadingCheckResultView check(Long articleId, ReadingCheckAnswerRequest request) {
        requirePublished(articleId);
        List<ReadingCheckItemView> items = new ArrayList<>();
        int total = 0;
        int score = 0;
        for (ReadingCheckAnswerRequest.Submission submission : request.answers()) {
            EnglishExercise exercise = requireExercise(submission.exerciseId());
            Long boundArticle = jdbc.queryForObject(
                    "SELECT article_id FROM english_reading_article_exercise WHERE exercise_id=?",
                    Long.class, submission.exerciseId());
            if (!Objects.equals(boundArticle, articleId) || !"PUBLISHED".equals(exercise.getPublishStatus())) {
                items.add(new ReadingCheckItemView(submission.exerciseId(), false, 0,
                        exercise.getScoreValue(), exercise.getExplanationMarkdown()));
                total += exercise.getScoreValue();
                continue;
            }
            boolean correct = isCorrect(exercise, submission.answer());
            int earned = correct ? exercise.getScoreValue() : 0;
            total += exercise.getScoreValue();
            score += earned;
            items.add(new ReadingCheckItemView(submission.exerciseId(), correct, earned,
                    exercise.getScoreValue(), exercise.getExplanationMarkdown()));
        }
        return new ReadingCheckResultView(score, total, items);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private boolean isCorrect(EnglishExercise exercise, Object submitted) {
        String kind = exerciseRules.kindOf(exercise.getQuestionType());
        JsonNode config = objectMapper.valueToTree(exercise.getConfigJson());
        JsonNode answer = config.get("answer");
        JsonNode answers = config.get("answers");
        switch (kind == null ? "" : kind) {
            case "CHOICE" -> {
                return answer != null && answer.isTextual()
                        && answer.asText().equals(String.valueOf(submitted));
            }
            case "TRUE_FALSE" -> {
                return answer != null && answer.isBoolean()
                        && answer.asBoolean() == Boolean.TRUE.equals(submitted);
            }
            case "FILL" -> {
                return matchesFill(answer, answers, submitted);
            }
            case "ORDER" -> {
                JsonNode correct = answer != null ? answer : config.get("items");
                return correct != null && correct.equals(objectMapper.valueToTree(submitted));
            }
            case "MATCH" -> {
                JsonNode correct = answer != null ? answer : config.get("pairs");
                return correct != null && correct.equals(objectMapper.valueToTree(submitted));
            }
            case "STRUCTURE", "MINIMAL_PAIR" -> {
                JsonNode correct = answer != null ? answer : config.get("pair");
                return correct != null && correct.equals(objectMapper.valueToTree(submitted));
            }
            default -> {
                return false;
            }
        }
    }

    private boolean matchesFill(JsonNode answer, JsonNode answers, Object submitted) {
        if (submitted == null) return false;
        String text = String.valueOf(submitted);
        if (answer != null && answer.isTextual() && answer.asText().equalsIgnoreCase(text)) return true;
        if (answers != null && answers.isArray()) {
            for (JsonNode node : answers) {
                if (node.isTextual() && node.asText().equalsIgnoreCase(text)) return true;
            }
        }
        return false;
    }

    private String submittedAsJson(Object submitted) {
        try {
            return objectMapper.writeValueAsString(submitted);
        } catch (Exception ex) {
            return "null";
        }
    }

    private Map<String, Object> parseConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_EXERCISE_CONFIG_INVALID",
                    "Invalid exercise config", "config_json is not valid JSON.");
        }
    }

    private Map<String, Object> sanitize(JsonNode config) {
        Map<String, Object> result = new LinkedHashMap<>();
        config.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (isAnswerBearing(key)) return;
            result.put(key, objectMapper.convertValue(entry.getValue(), Object.class));
        });
        return result;
    }

    private boolean isAnswerBearing(String key) {
        return key.equals("answer") || key.equals("answers") || key.equals("correctIndexes")
                || key.equals("correctOrder") || key.equals("standardOrder") || key.equals("answerKeys");
    }

    private ReadingExercisePublicView toPublic(java.sql.ResultSet rs) throws java.sql.SQLException {
        JsonNode config = readConfig(rs.getString("config_json"));
        return new ReadingExercisePublicView(rs.getLong("id"), rs.getString("question_type"),
                rs.getString("prompt_markdown"), sanitize(config), rs.getInt("score_value"),
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

    private ReadingExerciseView adminView(Long exerciseId, Long articleId) {
        return jdbc.queryForObject("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_exercise e WHERE e.id=?
                """, (rs, row) -> mapAdmin(rs, articleId), exerciseId);
    }

    private void bind(Long articleId, Long exerciseId) {
        jdbc.update("INSERT INTO english_reading_article_exercise(article_id,exercise_id) VALUES (?,?)",
                articleId, exerciseId);
    }

    private Long nextSort(Long articleId) {
        Integer max = jdbc.queryForObject("""
                SELECT COALESCE(MAX(e.sort_order),0) FROM english_reading_article_exercise ae
                JOIN english_exercise e ON e.id=ae.exercise_id WHERE ae.article_id=?
                """, Integer.class, articleId);
        return (long) ((max == null ? 0 : max) + 10);
    }

    private void requireBinding(Long articleId, Long exerciseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article_exercise WHERE article_id=? AND exercise_id=?",
                Integer.class, articleId, exerciseId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise is not bound to this article.");
        }
    }

    private EnglishExercise requireExercise(Long exerciseId) {
        EnglishExercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise does not exist.");
        }
        return exercise;
    }

    private void requireArticle(Long articleId) {
        articleService.get(articleId);
    }

    private void requirePublished(Long articleId) {
        String status = jdbc.queryForObject(
                "SELECT publish_status FROM english_reading_article WHERE id=?", String.class, articleId);
        if (!"PUBLISHED".equals(status)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Article not available", "The article is not published.");
        }
    }

    private String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

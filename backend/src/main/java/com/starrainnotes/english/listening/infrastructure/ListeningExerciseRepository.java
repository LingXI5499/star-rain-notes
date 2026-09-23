package com.starrainnotes.english.listening.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.domain.ListeningContentPort;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckItemView;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listening exercises bound to a single item (阶段三 §四.3).
 *
 * <p>Each exercise belongs to exactly one item. Public reads are sanitized and
 * scored via the shared {@link EnglishExerciseSafety} (no second copy). Binding
 * ownership is enforced server-side per 归属安全 rules: unbound / cross-item /
 * unpublished / duplicate exercise ids all return 422, never 500.</p>
 */
@Repository
public class ListeningExerciseRepository {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String LISTENING = "LISTENING";

    private final JdbcTemplate jdbc;
    private final EnglishExerciseMapper exerciseMapper;
    private final EnglishExerciseService exerciseRules;
    private final EnglishExerciseSafety safety;
    private final ListeningContentPort itemService;
    private final ObjectMapper objectMapper;
    private final SiteSettingsTimezone timezone;

    public ListeningExerciseRepository(JdbcTemplate jdbc, EnglishExerciseMapper exerciseMapper,
                                    EnglishExerciseService exerciseRules, EnglishExerciseSafety safety,
                                    ListeningContentPort itemService, ObjectMapper objectMapper,
                                    SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.exerciseMapper = exerciseMapper;
        this.exerciseRules = exerciseRules;
        this.safety = safety;
        this.itemService = itemService;
        this.objectMapper = objectMapper;
        this.timezone = timezone;
    }

    public List<ExerciseView> listByItem(Long itemId) {
        itemService.requireExists(itemId);
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_listening_item_exercise ie
                JOIN english_exercise e ON e.id=ie.exercise_id
                WHERE ie.listening_item_id=? ORDER BY e.sort_order, e.id
                """, (rs, row) -> mapAdmin(rs, itemId), itemId);
    }

    @Transactional
    public ExerciseView create(Long itemId, ExerciseRequest request) {
        itemService.requireExists(itemId);
        exerciseRules.validateConfig(LISTENING, request.questionType(), request.configJson());
        Long order = (long) (nextSort(itemId) * 10);
        EnglishExercise exercise = new EnglishExercise();
        exercise.setModuleType(LISTENING);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        exercise.setSortOrder(order.intValue());
        exercise.setPublishStatus(request.publishStatus() == null ? "DRAFT" : request.publishStatus());
        exerciseMapper.insert(exercise);
        jdbc.update("INSERT INTO english_listening_item_exercise(listening_item_id,exercise_id) VALUES (?,?)",
                itemId, exercise.getId());
        return adminView(exercise.getId(), itemId);
    }

    @Transactional
    public ExerciseView update(Long itemId, Long exerciseId, ExerciseRequest request) {
        requireBinding(itemId, exerciseId);
        exerciseRules.validateConfig(LISTENING, request.questionType(), request.configJson());
        EnglishExercise exercise = requireExercise(exerciseId);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        if (request.publishStatus() != null) exercise.setPublishStatus(request.publishStatus());
        exerciseMapper.updateById(exercise);
        return adminView(exerciseId, itemId);
    }

    @Transactional
    public void move(Long itemId, Long exerciseId, int targetIndex) {
        requireBinding(itemId, exerciseId);
        List<Long> ids = new ArrayList<>(jdbc.queryForList("""
                SELECT e.id FROM english_listening_item_exercise ie
                JOIN english_exercise e ON e.id=ie.exercise_id
                WHERE ie.listening_item_id=? ORDER BY e.sort_order, e.id
                """, Long.class, itemId));
        ids.remove(exerciseId);
        ids.add(Math.min(Math.max(targetIndex, 0), ids.size()), exerciseId);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_exercise SET sort_order=100000 WHERE id IN (" + joinIds(ids) + ")");
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_exercise SET sort_order=? WHERE id=?", (i + 1) * 10, ids.get(i));
        }
    }

    @Transactional
    public void delete(Long itemId, Long exerciseId) {
        requireBinding(itemId, exerciseId);
        jdbc.update("DELETE FROM english_listening_item_exercise WHERE listening_item_id=? AND exercise_id=?",
                itemId, exerciseId);
        exerciseMapper.deleteById(exerciseId);
    }

    public List<ExercisePublicView> publicListPublished(Long itemId) {
        itemService.requirePublished(itemId);
        return jdbc.query("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.score_value,e.sort_order
                FROM english_listening_item_exercise ie
                JOIN english_exercise e ON e.id=ie.exercise_id
                WHERE ie.listening_item_id=? AND e.publish_status='PUBLISHED'
                ORDER BY e.sort_order, e.id
                """, (rs, row) -> toPublic(rs), itemId);
    }

    @Transactional
    public CheckResultView check(Long itemId, CheckAnswerRequest request) {
        itemService.requirePublished(itemId);
        List<CheckItemView> items = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        int total = 0;
        int score = 0;
        for (CheckAnswerRequest.Submission submission : request.answers()) {
            if (!seen.add(submission.exerciseId())) {
                throw invalidAnswer("The same exercise cannot be submitted more than once.");
            }
            EnglishExercise exercise = requireOwnedPublished(itemId, submission.exerciseId());
            boolean correct = safety.isCorrect(exercise.getQuestionType(),
                    objectMapper.valueToTree(exercise.getConfigJson()), submission.answer());
            int earned = correct ? exercise.getScoreValue() : 0;
            total += exercise.getScoreValue();
            score += earned;
            items.add(new CheckItemView(submission.exerciseId(), correct, earned,
                    exercise.getScoreValue(), exercise.getExplanationMarkdown()));
        }
        return new CheckResultView(score, total, items);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private EnglishExercise requireOwnedPublished(Long itemId, Long exerciseId) {
        List<Long> ids = jdbc.queryForList("""
                SELECT e.id FROM english_listening_item_exercise ie
                JOIN english_exercise e ON e.id=ie.exercise_id
                WHERE ie.listening_item_id=? AND e.id=? AND e.module_type='LISTENING'
                  AND e.publish_status='PUBLISHED'
                """, Long.class, itemId, exerciseId);
        if (ids.isEmpty()) {
            throw invalidAnswer("The submitted exercise is not a published exercise of this item.");
        }
        return requireExercise(ids.get(0));
    }

    private void requireBinding(Long itemId, Long exerciseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item_exercise WHERE listening_item_id=? AND exercise_id=?",
                Integer.class, itemId, exerciseId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise is not bound to this item.");
        }
    }

    private EnglishExercise requireExercise(Long exerciseId) {
        EnglishExercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_EXERCISE_NOT_FOUND",
                    "Exercise not found", "The exercise does not exist.");
        }
        return exercise;
    }

    private Integer nextSort(Long itemId) {
        Integer max = jdbc.queryForObject("""
                SELECT COALESCE(MAX(e.sort_order),0) FROM english_listening_item_exercise ie
                JOIN english_exercise e ON e.id=ie.exercise_id WHERE ie.listening_item_id=?
                """, Integer.class, itemId);
        return max == null ? 1 : max / 10 + 1;
    }

    private Map<String, Object> parseConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_EXERCISE_CONFIG_INVALID",
                    "Invalid exercise config", "config_json is not valid JSON.");
        }
    }

    private ExercisePublicView toPublic(java.sql.ResultSet rs) throws java.sql.SQLException {
        JsonNode config = readConfig(rs.getString("config_json"));
        long id = rs.getLong("id");
        String q = rs.getString("question_type");
        return new ExercisePublicView(id, q, rs.getString("prompt_markdown"),
                safety.sanitize(q, config, id), rs.getInt("score_value"), rs.getInt("sort_order"));
    }

    private ExerciseView mapAdmin(java.sql.ResultSet rs, Long itemId) throws java.sql.SQLException {
        JsonNode config = readConfig(rs.getString("config_json"));
        return new ExerciseView(rs.getLong("id"), itemId, rs.getString("question_type"),
                rs.getString("prompt_markdown"), objectMapper.convertValue(config, new TypeReference<>() { }),
                rs.getString("explanation_markdown"), rs.getInt("score_value"), rs.getInt("sort_order"),
                rs.getString("publish_status"), format(rs.getTimestamp("updated_at")));
    }

    private JsonNode readConfig(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private ExerciseView adminView(Long exerciseId, Long itemId) {
        return jdbc.queryForObject("""
                SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,
                       e.score_value,e.sort_order,e.publish_status,e.updated_at
                FROM english_exercise e WHERE e.id=?
                """, (rs, row) -> mapAdmin(rs, itemId), exerciseId);
    }

    private ApiException invalidAnswer(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_ANSWER_INVALID",
                "Invalid listening answer", detail);
    }

    private String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    private String clean(String value) {
        return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
    }
}

package com.starrainnotes.english.reading.application;

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
import com.starrainnotes.english.reading.infrastructure.ReadingExerciseRepository;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reading exercise use cases; SQL remains in ReadingExerciseRepository. */
@Service
public class ReadingExerciseApplicationService {
    private static final String READING = "READING";
    private final ReadingExerciseRepository repository;
    private final EnglishExerciseService exerciseRules;
    private final EnglishExerciseSafety safety;
    private final ObjectMapper objectMapper;
    public ReadingExerciseApplicationService(ReadingExerciseRepository repository,
            EnglishExerciseService exerciseRules, EnglishExerciseSafety safety, ObjectMapper objectMapper) {
        this.repository = repository;
        this.exerciseRules = exerciseRules;
        this.safety = safety;
        this.objectMapper = objectMapper;
    }

    public List<ReadingExerciseView> listByArticle(Long articleId) {
        repository.requireArticle(articleId);
        return repository.listByArticle(articleId);
    }

    @Transactional
    public ReadingExerciseView create(Long articleId, ReadingExerciseRequest request) {
        repository.requireArticle(articleId);
        exerciseRules.validateConfig(READING, request.questionType(), request.configJson());
        Long exerciseId = repository.nextSort(articleId);
        EnglishExercise exercise = new EnglishExercise();
        exercise.setModuleType(READING);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        exercise.setSortOrder(exerciseId.intValue());
        exercise.setPublishStatus(request.publishStatus() == null ? "DRAFT" : request.publishStatus());
        repository.insert(exercise);
        repository.bind(articleId, exercise.getId());
        return repository.adminView(exercise.getId(), articleId);
    }

    @Transactional
    public ReadingExerciseView update(Long articleId, Long exerciseId, ReadingExerciseRequest request) {
        repository.requireArticle(articleId);
        repository.requireBinding(articleId, exerciseId);
        exerciseRules.validateConfig(READING, request.questionType(), request.configJson());
        EnglishExercise exercise = repository.requireExercise(exerciseId);
        exercise.setQuestionType(request.questionType());
        exercise.setPromptMarkdown(request.promptMarkdown());
        exercise.setConfigJson(parseConfig(request.configJson()));
        exercise.setExplanationMarkdown(clean(request.explanationMarkdown()));
        exercise.setScoreValue(request.scoreValue());
        if (request.publishStatus() != null) exercise.setPublishStatus(request.publishStatus());
        repository.update(exercise);
        return repository.adminView(exerciseId, articleId);
    }

    @Transactional
    public void delete(Long articleId, Long exerciseId) {
        repository.requireBinding(articleId, exerciseId);
        repository.deleteBinding(articleId, exerciseId);
        repository.delete(exerciseId);
    }

    @Transactional
    public void setPublishStatus(Long articleId, Long exerciseId, boolean published) {
        repository.requireBinding(articleId, exerciseId);
        EnglishExercise exercise = repository.requireExercise(exerciseId);
        exercise.setPublishStatus(published ? "PUBLISHED" : "DRAFT");
        repository.update(exercise);
    }

    public List<ReadingExercisePublicView> publicListPublished(Long articleId) {
        repository.requirePublished(articleId);
        return repository.publicListPublished(articleId);
    }

    @Transactional
    public ReadingCheckResultView check(Long articleId, ReadingCheckAnswerRequest request) {
        repository.requirePublished(articleId);
        List<ReadingCheckItemView> items = new ArrayList<>();
        Set<Long> submittedIds = new LinkedHashSet<>();
        int total = 0;
        int score = 0;
        for (ReadingCheckAnswerRequest.Submission submission : request.answers()) {
            if (!submittedIds.add(submission.exerciseId())) {
                throw invalidAnswer("The same exercise cannot be submitted more than once.");
            }
            EnglishExercise exercise = repository.requirePublishedBinding(articleId, submission.exerciseId());
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
        JsonNode config = objectMapper.valueToTree(exercise.getConfigJson());
        return safety.isCorrect(exercise.getQuestionType(), config, submitted);
    }

    private Map<String, Object> parseConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_EXERCISE_CONFIG_INVALID",
                    "Invalid exercise config", "config_json is not valid JSON.");
        }
    }

    @Transactional
    public void move(Long articleId, Long exerciseId, int targetIndex) {
        repository.move(articleId, exerciseId, targetIndex);
    }

    private ApiException invalidAnswer(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_ANSWER_INVALID",
                "Invalid reading answer", detail);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.starrainnotes.english.writing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckItemView;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import com.starrainnotes.english.writing.infrastructure.WritingExerciseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WritingExerciseApplicationService {
    private final WritingExerciseRepository repository;
    private final EnglishExerciseSafety safety;
    private final ObjectMapper json;

    public WritingExerciseApplicationService(WritingExerciseRepository repository,
                                             EnglishExerciseSafety safety, ObjectMapper json) {
        this.repository = repository;
        this.safety = safety;
        this.json = json;
    }

    public List<ExerciseView> list(Long promptId) {
        return repository.list(promptId);
    }

    @Transactional
    public ExerciseView create(Long promptId, ExerciseRequest request) {
        return repository.create(promptId, request);
    }

    @Transactional
    public ExerciseView update(Long promptId, Long exerciseId, ExerciseRequest request) {
        return repository.update(promptId, exerciseId, request);
    }

    @Transactional
    public void move(Long promptId, Long exerciseId, int index) {
        repository.move(promptId, exerciseId, index);
    }

    @Transactional
    public void delete(Long promptId, Long exerciseId) {
        repository.delete(promptId, exerciseId);
    }

    public List<ExercisePublicView> publicList(String slug) {
        return repository.publicList(slug);
    }

    @Transactional
    public CheckResultView check(String slug, CheckAnswerRequest request) {
        Long promptId = repository.publishedPrompt(slug);
        int score = 0;
        int total = 0;
        List<CheckItemView> items = new ArrayList<>();
        Set<Long> unique = new HashSet<>();
        for (CheckAnswerRequest.Submission submission : request.answers()) {
            if (!unique.add(submission.exerciseId())) {
                throw invalid("The same exercise cannot be submitted twice.");
            }
            EnglishExercise exercise = repository.publishedBinding(promptId, submission.exerciseId());
            boolean correct = safety.isCorrect(exercise.getQuestionType(),
                    json.valueToTree(exercise.getConfigJson()), submission.answer());
            int earned = correct ? exercise.getScoreValue() : 0;
            score += earned;
            total += exercise.getScoreValue();
            items.add(new CheckItemView(exercise.getId(), correct, earned,
                    exercise.getScoreValue(), exercise.getExplanationMarkdown()));
        }
        return new CheckResultView(score, total, items);
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_WRITING_ANSWER_INVALID",
                "Invalid writing request", detail);
    }
}

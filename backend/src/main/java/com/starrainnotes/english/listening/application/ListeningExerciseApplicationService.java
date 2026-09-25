package com.starrainnotes.english.listening.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.infrastructure.ListeningExerciseRepository;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckItemView;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Listening exercise use cases. */
@Service
public class ListeningExerciseApplicationService {
    private final ListeningExerciseRepository repository;
    private final EnglishExerciseSafety safety;
    private final ObjectMapper objectMapper;
    public ListeningExerciseApplicationService(ListeningExerciseRepository repository,
                                               EnglishExerciseSafety safety, ObjectMapper objectMapper) {
        this.repository = repository;
        this.safety = safety;
        this.objectMapper = objectMapper;
    }
    public List<ExerciseView> listByItem(Long id) { return repository.listByItem(id); }
    @Transactional public ExerciseView create(Long id, ExerciseRequest request) {
        return repository.create(id, request);
    }
    @Transactional public ExerciseView update(Long id, Long exerciseId, ExerciseRequest request) {
        return repository.update(id, exerciseId, request);
    }
    @Transactional public void move(Long id, Long exerciseId, int targetIndex) {
        repository.move(id, exerciseId, targetIndex);
    }
    @Transactional public void delete(Long id, Long exerciseId) { repository.delete(id, exerciseId); }
    public List<ExercisePublicView> publicListPublished(Long id) {
        return repository.publicListPublished(id);
    }
    @Transactional public CheckResultView check(Long id, CheckAnswerRequest request) {
        repository.requirePublishedItem(id);
        List<CheckItemView> items = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        int total = 0;
        int score = 0;
        for (CheckAnswerRequest.Submission submission : request.answers()) {
            if (!seen.add(submission.exerciseId())) {
                throw invalidAnswer("The same exercise cannot be submitted more than once.");
            }
            EnglishExercise exercise = repository.requireOwnedPublished(id, submission.exerciseId());
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

    private ApiException invalidAnswer(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_ANSWER_INVALID",
                "Invalid listening answer", detail);
    }
}

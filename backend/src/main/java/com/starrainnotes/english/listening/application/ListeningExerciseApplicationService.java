package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.infrastructure.ListeningExerciseRepository;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Listening exercise use cases. */
@Service
public class ListeningExerciseApplicationService {
    private final ListeningExerciseRepository repository;
    public ListeningExerciseApplicationService(ListeningExerciseRepository repository) {
        this.repository = repository;
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
        return repository.check(id, request);
    }
}

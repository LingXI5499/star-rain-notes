package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.shared.exercise.dto.*;
import com.starrainnotes.english.writing.infrastructure.WritingExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WritingExerciseApplicationService {
    private final WritingExerciseRepository repository;
    public WritingExerciseApplicationService(WritingExerciseRepository repository) { this.repository = repository; }
    public List<ExerciseView> list(Long promptId) { return repository.list(promptId); }
    @Transactional public ExerciseView create(Long promptId,ExerciseRequest request) { return repository.create(promptId,request); }
    @Transactional public ExerciseView update(Long promptId,Long exerciseId,ExerciseRequest request) { return repository.update(promptId,exerciseId,request); }
    @Transactional public void move(Long promptId,Long exerciseId,int index) { repository.move(promptId,exerciseId,index); }
    @Transactional public void delete(Long promptId,Long exerciseId) { repository.delete(promptId,exerciseId); }
    public List<ExercisePublicView> publicList(String slug) { return repository.publicList(slug); }
    @Transactional public CheckResultView check(String slug,CheckAnswerRequest request) { return repository.check(slug,request); }
}

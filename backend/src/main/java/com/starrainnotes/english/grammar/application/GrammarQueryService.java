package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.infrastructure.GrammarRepository;
import org.springframework.stereotype.Service;

/** Read use cases for the singleton Grammar course and its curriculum. */
@Service
public class GrammarQueryService {
    private final GrammarRepository repository;
    public GrammarQueryService(GrammarRepository repository) { this.repository = repository; }
    public GrammarCourseView course() { return repository.course(); }
    public GrammarCourseView publicCourse() { return repository.publicCourse(); }
    public GrammarCurriculumView curriculum() { return repository.curriculum(); }
    public GrammarCurriculumView publicCurriculum() { return repository.publicCurriculum(); }
    public GrammarLessonDetailView lesson(long id) { return repository.lesson(id); }
    public GrammarLessonDetailView publicLesson(String slug) { return repository.publicLesson(slug); }
    public long publishedCount() { return repository.publishedCount(); }
}

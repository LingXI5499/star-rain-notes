package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.dto.UpdateGrammarCourseRequest;
import com.starrainnotes.english.grammar.infrastructure.GrammarRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Write use cases for Grammar course, sections and lessons. */
@Service
public class GrammarCommandService {
    private final GrammarRepository repository;
    private final GrammarOrderingService ordering;
    public GrammarCommandService(GrammarRepository repository, GrammarOrderingService ordering) {
        this.repository = repository;
        this.ordering = ordering;
    }
    public GrammarCourseView updateCourse(UpdateGrammarCourseRequest request) { return repository.updateCourse(request); }
    public GrammarCourseView publishCourse() { return repository.publishCourse(); }
    public GrammarCourseView withdrawCourse() { return repository.withdrawCourse(); }
    public GrammarSectionView createSection(GrammarSectionRequest request) { return repository.createSection(request); }
    public GrammarSectionView updateSection(long id, GrammarSectionRequest request) { return repository.updateSection(id, request); }
    public void deleteSection(long id) { repository.deleteSection(id); }
    public GrammarLessonDetailView createLesson(GrammarLessonRequest request) { return repository.createLesson(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.UPDATED)
    public GrammarLessonDetailView updateLesson(long id, GrammarLessonRequest request) {
        return repository.updateLesson(id, request);
    }
    public void deleteLesson(long id) { repository.deleteLesson(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.PUBLISHED)
    public GrammarLessonDetailView publishLesson(long id) { return repository.publishLesson(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.WITHDRAWN)
    public GrammarLessonDetailView withdrawLesson(long id) { return repository.withdrawLesson(id); }
    @Transactional public void moveSection(long id, com.starrainnotes.english.grammar.dto.GrammarMoveRequest request) {
        ordering.moveSection(id, request);
    }
    @Transactional public void moveLesson(long id, com.starrainnotes.english.grammar.dto.GrammarMoveRequest request) {
        ordering.moveLesson(id, request);
    }
    @Transactional public void reassignLesson(long id, com.starrainnotes.english.grammar.dto.GrammarReassignRequest request) {
        ordering.reassignLesson(id, request);
    }
}

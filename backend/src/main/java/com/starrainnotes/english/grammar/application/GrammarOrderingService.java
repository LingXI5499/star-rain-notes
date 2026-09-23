package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.dto.GrammarMoveRequest;
import com.starrainnotes.english.grammar.dto.GrammarReassignRequest;
import com.starrainnotes.english.grammar.infrastructure.GrammarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Ordering and cross-section reassignment use cases. */
@Service
public class GrammarOrderingService {
    private final GrammarRepository repository;
    public GrammarOrderingService(GrammarRepository repository) { this.repository = repository; }

    @Transactional
    public void moveSection(long sectionId, GrammarMoveRequest request) {
        repository.requireSection(sectionId);
        List<Long> ids = repository.loadSectionIds();
        ids.remove(sectionId);
        ids.add(Math.min(request.targetIndex(), ids.size()), sectionId);
        repository.normalizeSections(ids);
    }

    @Transactional
    public void moveLesson(long lessonId, GrammarMoveRequest request) {
        long sectionId = repository.lesson(lessonId).sectionId();
        List<Long> ids = repository.loadLessonIds(sectionId);
        ids.remove(lessonId);
        ids.add(Math.min(request.targetIndex(), ids.size()), lessonId);
        repository.normalizeLessons(sectionId, ids);
    }

    @Transactional
    public void reassignLesson(long lessonId, GrammarReassignRequest request) {
        long sourceSectionId = repository.lesson(lessonId).sectionId();
        long targetSectionId = request.targetSectionId();
        repository.requireSection(targetSectionId);
        if (sourceSectionId == targetSectionId) return;

        List<Long> source = repository.loadLessonIds(sourceSectionId);
        source.remove(lessonId);
        repository.normalizeLessons(sourceSectionId, source);

        List<Long> target = repository.loadLessonIds(targetSectionId);
        target.add(lessonId);
        repository.assignLessonSection(lessonId, targetSectionId, target.size() * 10);
        repository.normalizeLessons(targetSectionId, target);
    }
}

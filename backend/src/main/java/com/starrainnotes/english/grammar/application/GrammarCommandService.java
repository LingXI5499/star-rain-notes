package com.starrainnotes.english.grammar.application;

import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.grammar.domain.GrammarPublishPolicy;
import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.shared.review.EnglishUpdateReview;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.dto.UpdateGrammarCourseRequest;
import com.starrainnotes.english.grammar.infrastructure.GrammarRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Write use cases for Grammar course, sections and lessons. */
@Service
public class GrammarCommandService {
    private final GrammarRepository repository;
    private final GrammarPublishPolicy policy;
    private final MediaPort media;
    private final GrammarOrderingService ordering;
    private final EnglishUpdateReview reviews;
    public GrammarCommandService(GrammarRepository repository, GrammarPublishPolicy policy, MediaPort media,
                                 GrammarOrderingService ordering, EnglishUpdateReview reviews) {
        this.repository = repository;
        this.policy = policy;
        this.media = media;
        this.ordering = ordering;
        this.reviews = reviews;
    }
    @Transactional
    public GrammarCourseView updateCourse(UpdateGrammarCourseRequest request) {
        Long coverId = request.coverMediaId();
        policy.requireCover(coverId, coverId != null && media.isType(coverId, "IMAGE"));
        return repository.updateCourse(request);
    }
    @Transactional
    public GrammarCourseView publishCourse() { return repository.publishCourse(); }
    @Transactional
    public GrammarCourseView withdrawCourse() {
        policy.validateCourseWithdrawal(repository.course().publishStatus());
        return repository.withdrawCourse();
    }
    @Transactional
    public GrammarSectionView createSection(GrammarSectionRequest request) { return repository.createSection(request); }
    @Transactional
    public GrammarSectionView updateSection(long id, GrammarSectionRequest request) {
        return repository.updateSection(id, request);
    }
    @Transactional
    public void deleteSection(long id) {
        policy.requireEmptySection(repository.countLessons(id));
        repository.deleteSection(id);
    }
    @Transactional
    public GrammarLessonDetailView createLesson(GrammarLessonRequest request) {
        try {
            return repository.createLesson(request);
        } catch (DuplicateKeyException ex) {
            throw policy.slugConflict();
        }
    }
    @Transactional
    public ReviewSubmissionResult updateLessonForEditor(Long actorId, boolean superAdmin, long id,
                                                        GrammarLessonRequest request) {
        return reviews.decide(superAdmin, "ENGLISH_GRAMMAR_LESSON", id, request.title(), request, actorId,
                () -> updateLesson(id, request));
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.UPDATED)
    public GrammarLessonDetailView updateLesson(long id, GrammarLessonRequest request) {
        policy.requireSameSection(repository.lesson(id).sectionId(), request.sectionId());
        try {
            return repository.updateLesson(id, request);
        } catch (DuplicateKeyException ex) {
            throw policy.slugConflict();
        }
    }
    @Transactional
    public void deleteLesson(long id) { repository.deleteLesson(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.PUBLISHED)
    public GrammarLessonDetailView publishLesson(long id) { return repository.publishLesson(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.GRAMMAR_LESSON, changeType = EnglishContentChangeType.WITHDRAWN)
    public GrammarLessonDetailView withdrawLesson(long id) {
        policy.validateLessonWithdrawal(repository.lesson(id).publishStatus());
        return repository.withdrawLesson(id);
    }
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

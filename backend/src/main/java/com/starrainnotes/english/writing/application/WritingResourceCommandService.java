package com.starrainnotes.english.writing.application;

import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import com.starrainnotes.english.writing.domain.WritingResourcePolicy;
import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import com.starrainnotes.english.shared.review.EnglishUpdateReview;
import com.starrainnotes.english.writing.dto.WritingResourceView;
import com.starrainnotes.english.writing.infrastructure.WritingResourceRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WritingResourceCommandService {
    private final WritingResourceRepository repository;
    private final WritingResourcePolicy policy;
    private final EnglishUpdateReview reviews;
    public WritingResourceCommandService(WritingResourceRepository repository, WritingResourcePolicy policy,
                                         EnglishUpdateReview reviews) {
        this.repository = repository;
        this.policy = policy;
        this.reviews = reviews;
    }
    public ReviewSubmissionResult updateForEditor(Long actorId, boolean superAdmin, Long id, WritingResourceRequest request) {
        return reviews.decide(superAdmin, "ENGLISH_WRITING_RESOURCE", id, request.title(), request, actorId,
                () -> update(id, request));
    }
    @Transactional
    public WritingResourceView create(WritingResourceRequest request) { return repository.create(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.UPDATED)
    public WritingResourceView update(Long id,WritingResourceRequest request) { return repository.update(id,request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.PUBLISHED)
    public WritingResourceView publish(Long id) {
        WritingResourceView resource = repository.get(id);
        policy.validatePublication(resource.title(), resource.bodyMarkdown(),
                repository.hasEnabledTag(id, "TOPIC") || repository.hasEnabledTag(id, "GENRE"),
                resource.resourceKind(), resource.templateSchemaJson());
        return repository.publish(id);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.WITHDRAWN)
    public WritingResourceView withdraw(Long id) { return repository.withdraw(id); }
    @Transactional
    public void delete(Long id) { repository.delete(id); }
    @Transactional
    public void move(Long id,int target) { repository.move(id,target); }
}

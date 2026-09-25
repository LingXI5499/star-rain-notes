package com.starrainnotes.english.writing.application;

import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import com.starrainnotes.english.writing.domain.WritingPromptPolicy;
import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import com.starrainnotes.english.shared.review.EnglishUpdateReview;
import com.starrainnotes.english.writing.dto.WritingPromptView;
import com.starrainnotes.english.writing.infrastructure.WritingPromptRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WritingPromptCommandService {
    private final WritingPromptRepository repository;
    private final WritingPromptPolicy policy;
    private final EnglishUpdateReview reviews;
    public WritingPromptCommandService(WritingPromptRepository repository, WritingPromptPolicy policy,
                                       EnglishUpdateReview reviews) {
        this.repository = repository;
        this.policy = policy;
        this.reviews = reviews;
    }
    public ReviewSubmissionResult updateForEditor(Long actorId, boolean superAdmin, Long id, WritingPromptRequest request) {
        return reviews.decide(superAdmin, "ENGLISH_WRITING_PROMPT", id, request.title(), request, actorId,
                () -> update(id, request));
    }
    @Transactional
    public WritingPromptView create(WritingPromptRequest request) { return repository.create(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.UPDATED)
    public WritingPromptView update(Long id,WritingPromptRequest request) { return repository.update(id,request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.PUBLISHED)
    public WritingPromptView publish(Long id) {
        WritingPromptView prompt = repository.get(id);
        policy.validatePublication(prompt.backgroundMarkdown(), prompt.requirementsMarkdown(),
                repository.hasTopicOrGenre(id),
                repository.publishedResource(prompt.templateResourceId(), "TEMPLATE"),
                repository.publishedResource(prompt.modelResourceId(), "MODEL_ESSAY"),
                prompt.templateResourceId(), prompt.modelResourceId());
        return repository.publish(id);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.WITHDRAWN)
    public WritingPromptView withdraw(Long id) { return repository.withdraw(id); }
    @Transactional
    public void delete(Long id) { repository.delete(id); }
    @Transactional
    public void move(Long id,int target) { repository.move(id,target); }
}

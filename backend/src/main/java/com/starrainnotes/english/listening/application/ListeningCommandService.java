package com.starrainnotes.english.listening.application;

import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.shared.review.EnglishUpdateReview;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.listening.domain.ListeningPublishPolicy;
import com.starrainnotes.english.listening.infrastructure.ListeningRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional entry points for listening content changes. */
@Service
public class ListeningCommandService {
    private final ListeningRepository repository;
    private final ListeningPublishPolicy policy;
    private final MediaPort media;
    private final EnglishUpdateReview reviews;
    public ListeningCommandService(ListeningRepository repository, ListeningPublishPolicy policy,
                                   MediaPort media, EnglishUpdateReview reviews) {
        this.repository = repository;
        this.policy = policy;
        this.media = media;
        this.reviews = reviews;
    }
    public ReviewSubmissionResult updateForEditor(Long actorId, boolean superAdmin, Long id, ListeningItemRequest request) {
        return reviews.decide(superAdmin, "ENGLISH_LISTENING_ITEM", id, request.title(), request, actorId,
                () -> update(id, request));
    }
    @Transactional public ListeningItemView create(ListeningItemRequest request) {
        validateRequest(request);
        return repository.create(request);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.UPDATED)
    public ListeningItemView update(Long id, ListeningItemRequest request) {
        validateRequest(request);
        return repository.update(id, request);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.PUBLISHED)
    public ListeningItemView publish(Long id) { return repository.publish(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.WITHDRAWN)
    public ListeningItemView withdraw(Long id) {
        policy.requireWithdrawable(repository.get(id).publishStatus());
        return repository.withdraw(id);
    }
    @Transactional public void delete(Long id) {
        policy.requireDeletable(repository.get(id).publishStatus());
        repository.delete(id);
    }

    private void validateRequest(ListeningItemRequest request) {
        policy.requireLevel(request.listeningLevel());
        policy.requireKnownCefr(request.cefrLevel(), repository.cefrExists(request.cefrLevel()));
        Long audioId = request.audioMediaId();
        policy.requireAudio(audioId, audioId != null && media.isType(audioId, "AUDIO"));
        Long coverId = request.coverMediaId();
        policy.requireCover(coverId, coverId != null && media.isType(coverId, "IMAGE"));
    }
}

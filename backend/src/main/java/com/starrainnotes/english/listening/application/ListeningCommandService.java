package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.listening.dto.ListeningItemView;
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
    public ListeningCommandService(ListeningRepository repository) { this.repository = repository; }
    @Transactional public ListeningItemView create(ListeningItemRequest request) { return repository.create(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.UPDATED)
    public ListeningItemView update(Long id, ListeningItemRequest request) {
        return repository.update(id, request);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.PUBLISHED)
    public ListeningItemView publish(Long id) { return repository.publish(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.LISTENING, changeType = EnglishContentChangeType.WITHDRAWN)
    public ListeningItemView withdraw(Long id) { return repository.withdraw(id); }
    @Transactional public void delete(Long id) { repository.delete(id); }
}

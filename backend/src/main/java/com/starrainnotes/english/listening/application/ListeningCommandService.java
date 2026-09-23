package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.infrastructure.ListeningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional entry points for listening content changes. */
@Service
public class ListeningCommandService {
    private final ListeningRepository repository;
    public ListeningCommandService(ListeningRepository repository) { this.repository = repository; }
    @Transactional public ListeningItemView create(ListeningItemRequest request) { return repository.create(request); }
    @Transactional public ListeningItemView update(Long id, ListeningItemRequest request) {
        return repository.update(id, request);
    }
    @Transactional public ListeningItemView publish(Long id) { return repository.publish(id); }
    @Transactional public ListeningItemView withdraw(Long id) { return repository.withdraw(id); }
    @Transactional public void delete(Long id) { repository.delete(id); }
}

package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.ReadingPairRef;
import com.starrainnotes.english.listening.dto.ReadingPairRequest;
import com.starrainnotes.english.listening.infrastructure.ListeningRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Reading/listening relation use cases. */
@Service
public class ListeningRelationService {
    private final ListeningRelationRepository repository;
    public ListeningRelationService(ListeningRelationRepository repository) { this.repository = repository; }
    public List<ReadingPairRef> readingPairs(Long id) { return repository.readingPairs(id); }
    @Transactional public void addReadingPair(Long id, ReadingPairRequest request) {
        repository.addReadingPair(id, request);
    }
    @Transactional public void removeReadingPair(Long id, Long readingId) {
        repository.removeReadingPair(id, readingId);
    }
}

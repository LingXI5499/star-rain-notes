package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.infrastructure.ListeningRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reads the explicit Reading ↔ Listening relation owned by Listening. */
@Service
@Transactional(readOnly = true)
public class ListeningPairQueryService {
    private final ListeningRelationRepository repository;

    public ListeningPairQueryService(ListeningRelationRepository repository) {
        this.repository = repository;
    }

    public List<Long> listeningIdsForReading(long readingId) {
        return repository.listeningIdsForReading(readingId);
    }

    public List<Long> readingIdsForListening(long listeningId) {
        return repository.readingIdsForListening(listeningId);
    }
}

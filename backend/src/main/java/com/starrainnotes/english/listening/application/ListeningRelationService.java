package com.starrainnotes.english.listening.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.dto.ReadingPairRef;
import com.starrainnotes.english.listening.dto.ReadingPairRequest;
import com.starrainnotes.english.listening.infrastructure.ListeningRelationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Reading/listening relation use cases. */
@Service
public class ListeningRelationService {
    private final ListeningRelationRepository repository;
    public ListeningRelationService(ListeningRelationRepository repository) { this.repository = repository; }
    public List<ReadingPairRef> readingPairs(Long id) { return repository.readingPairs(id); }
    @Transactional public void addReadingPair(Long id, ReadingPairRequest request) {
        if (!Set.of("SAME_CONTENT", "SAME_TOPIC", "EXTENDED_TRAINING").contains(request.relationType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_PAIR_TYPE_INVALID",
                    "Invalid relation type", "relationType must be one of SAME_CONTENT/SAME_TOPIC/EXTENDED_TRAINING.");
        }
        repository.addReadingPair(id, request);
    }
    @Transactional public void removeReadingPair(Long id, Long readingId) {
        repository.removeReadingPair(id, readingId);
    }
}

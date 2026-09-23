package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.ListeningSegmentRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import com.starrainnotes.english.listening.infrastructure.ListeningSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Listening segment use cases. */
@Service
public class ListeningSegmentService {
    private final ListeningSegmentRepository repository;
    public ListeningSegmentService(ListeningSegmentRepository repository) { this.repository = repository; }
    public List<ListeningSegmentView> list(Long id) { return repository.list(id); }
    public List<ListeningSegmentView> publicList(Long id) { return repository.publicList(id); }
    @Transactional public ListeningSegmentView create(Long id, ListeningSegmentRequest request) {
        return repository.create(id, request);
    }
    @Transactional public ListeningSegmentView update(Long id, Long segmentId, ListeningSegmentRequest request) {
        return repository.update(id, segmentId, request);
    }
    @Transactional public void delete(Long id, Long segmentId) { repository.delete(id, segmentId); }
    @Transactional public void move(Long id, Long segmentId, int targetIndex) {
        repository.move(id, segmentId, targetIndex);
    }
    @Transactional public List<ListeningSegmentView> replaceBatch(Long id, List<ListeningSegmentRequest> requests) {
        return repository.replace(id, requests);
    }
}

package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.WritingResourceRequest;
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
    public WritingResourceCommandService(WritingResourceRepository repository) { this.repository = repository; }
    public WritingResourceView create(WritingResourceRequest request) { return repository.create(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.UPDATED)
    public WritingResourceView update(Long id,WritingResourceRequest request) { return repository.update(id,request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.PUBLISHED)
    public WritingResourceView publish(Long id) { return repository.publish(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_RESOURCE, changeType = EnglishContentChangeType.WITHDRAWN)
    public WritingResourceView withdraw(Long id) { return repository.withdraw(id); }
    public void delete(Long id) { repository.delete(id); }
    public void move(Long id,int target) { repository.move(id,target); }
}

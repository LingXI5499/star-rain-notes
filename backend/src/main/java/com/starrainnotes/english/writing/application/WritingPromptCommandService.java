package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.WritingPromptRequest;
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
    public WritingPromptCommandService(WritingPromptRepository repository) { this.repository = repository; }
    public WritingPromptView create(WritingPromptRequest request) { return repository.create(request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.UPDATED)
    public WritingPromptView update(Long id,WritingPromptRequest request) { return repository.update(id,request); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.PUBLISHED)
    public WritingPromptView publish(Long id) { return repository.publish(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.WRITING_PROMPT, changeType = EnglishContentChangeType.WITHDRAWN)
    public WritingPromptView withdraw(Long id) { return repository.withdraw(id); }
    public void delete(Long id) { repository.delete(id); }
    public void move(Long id,int target) { repository.move(id,target); }
}

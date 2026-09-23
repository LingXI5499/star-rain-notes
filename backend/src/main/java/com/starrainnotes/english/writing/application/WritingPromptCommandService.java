package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import com.starrainnotes.english.writing.dto.WritingPromptView;
import com.starrainnotes.english.writing.infrastructure.WritingPromptRepository;
import org.springframework.stereotype.Service;

@Service
public class WritingPromptCommandService {
    private final WritingPromptRepository repository;
    public WritingPromptCommandService(WritingPromptRepository repository) { this.repository = repository; }
    public WritingPromptView create(WritingPromptRequest request) { return repository.create(request); }
    public WritingPromptView update(Long id,WritingPromptRequest request) { return repository.update(id,request); }
    public WritingPromptView publish(Long id) { return repository.publish(id); }
    public WritingPromptView withdraw(Long id) { return repository.withdraw(id); }
    public void delete(Long id) { repository.delete(id); }
    public void move(Long id,int target) { repository.move(id,target); }
}

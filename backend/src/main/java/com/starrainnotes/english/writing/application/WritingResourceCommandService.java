package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import com.starrainnotes.english.writing.dto.WritingResourceView;
import com.starrainnotes.english.writing.infrastructure.WritingResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class WritingResourceCommandService {
    private final WritingResourceRepository repository;
    public WritingResourceCommandService(WritingResourceRepository repository) { this.repository = repository; }
    public WritingResourceView create(WritingResourceRequest request) { return repository.create(request); }
    public WritingResourceView update(Long id,WritingResourceRequest request) { return repository.update(id,request); }
    public WritingResourceView publish(Long id) { return repository.publish(id); }
    public WritingResourceView withdraw(Long id) { return repository.withdraw(id); }
    public void delete(Long id) { repository.delete(id); }
    public void move(Long id,int target) { repository.move(id,target); }
}

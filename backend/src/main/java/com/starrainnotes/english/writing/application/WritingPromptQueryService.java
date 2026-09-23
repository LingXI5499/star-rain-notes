package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.*;
import com.starrainnotes.english.writing.infrastructure.WritingPromptRepository;
import org.springframework.stereotype.Service;

@Service
public class WritingPromptQueryService {
    private final WritingPromptRepository repository;
    public WritingPromptQueryService(WritingPromptRepository repository) { this.repository = repository; }
    public WritingPromptView get(Long id) { return repository.get(id); }
    public WritingPromptView publicGet(String slug) { return repository.publicGet(slug); }
    public WritingPageView<WritingPromptSummaryView> list(int page,int size,String q,String cefr,String status,Long topic,Long genre) { return repository.list(page,size,q,cefr,status,topic,genre); }
    public WritingPageView<WritingPromptSummaryView> publicList(int page,int size,String q,String cefr,Long topic,Long genre) { return repository.publicList(page,size,q,cefr,topic,genre); }
}

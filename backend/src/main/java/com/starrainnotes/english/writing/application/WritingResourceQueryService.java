package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.writing.dto.*;
import com.starrainnotes.english.writing.infrastructure.WritingResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WritingResourceQueryService {
    private final WritingResourceRepository repository;
    public WritingResourceQueryService(WritingResourceRepository repository) { this.repository = repository; }
    public WritingResourceView get(Long id) { return repository.get(id); }
    public WritingResourceView publicGet(String slug) { return repository.publicGet(slug); }
    public WritingPageView<WritingResourceSummaryView> list(int page,int size,String q,String kind,String level,String cefr,String status,Long topic,Long genre) { return repository.list(page,size,q,kind,level,cefr,status,topic,genre); }
    public WritingPageView<WritingResourceSummaryView> publicList(int page,int size,String q,String kind,String level,String cefr,Long topic,Long genre) { return repository.publicList(page,size,q,kind,level,cefr,topic,genre); }
}

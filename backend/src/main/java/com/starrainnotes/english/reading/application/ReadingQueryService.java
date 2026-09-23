package com.starrainnotes.english.reading.application;

import com.starrainnotes.english.reading.dto.*;
import com.starrainnotes.english.reading.infrastructure.ReadingRepository;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read use cases for admin and public reading pages. */
@Service
@Transactional(readOnly = true)
public class ReadingQueryService {
    private final ReadingRepository repository;
    public ReadingQueryService(ReadingRepository repository) { this.repository = repository; }
    public ReadingArticleView get(Long id) { return repository.get(id); }
    public ReadingArticleView publicGet(String slug) { return repository.publicGet(slug); }
    public ReadingPageView list(int page, int pageSize, String q, String status,
                                Integer level, String cefr, Long topic, Long genre) {
        return repository.list(page, pageSize, q, status, level, cefr, topic, genre);
    }
    public ReadingPageView publicList(int page, int pageSize, String q, Integer level,
                                      String cefr, Long topic, Long genre) {
        return repository.publicList(page, pageSize, q, level, cefr, topic, genre);
    }
    public ReadingHomeView home() { return repository.home(); }
    public ContentCatalogSlice catalogDescriptors(ContentCatalogFilter filter, int limit) {
        return repository.catalogDescriptors(filter, limit);
    }
}

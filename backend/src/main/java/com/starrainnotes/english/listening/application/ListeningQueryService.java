package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.infrastructure.ListeningRecommendationTagRepository;
import com.starrainnotes.english.shared.content.TagMatchCandidate;
import java.util.List;

import com.starrainnotes.english.listening.dto.ListeningHomeView;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.dto.ListeningPageView;
import com.starrainnotes.english.listening.infrastructure.ListeningRepository;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read use cases for public and admin listening content. */
@Service
@Transactional(readOnly = true)
public class ListeningQueryService {
    private final ListeningRecommendationTagRepository recommendationTags;
    private final ListeningRepository repository;
    public ListeningQueryService(ListeningRepository repository,
                          ListeningRecommendationTagRepository recommendationTags) {
        this.repository = repository;
        this.recommendationTags = recommendationTags;
    }
    public List<Long> tagIds(long contentId) { return recommendationTags.tagIds(contentId); }
    public List<TagMatchCandidate> tagMatches(List<Long> termIds) { return recommendationTags.matches(termIds); }
    public ListeningItemView get(Long id) { return repository.get(id); }
    public ListeningItemView publicGet(String slug) { return repository.publicGet(slug); }
    public ListeningPageView list(int page, int pageSize, String q, String status, Integer level,
                                  String cefr, Long topic, Long scene, Long format) {
        return repository.list(page, pageSize, q, status, level, cefr, topic, scene, format);
    }
    public ListeningPageView publicList(int page, int pageSize, String q, Integer level, String cefr,
                                        Long topic, Long scene, Long format) {
        return repository.publicList(page, pageSize, q, level, cefr, topic, scene, format);
    }
    public ListeningHomeView home() { return repository.home(); }
    public ContentCatalogSlice catalogDescriptors(ContentCatalogFilter filter, int limit) {
        return repository.catalogDescriptors(filter, limit);
    }
}

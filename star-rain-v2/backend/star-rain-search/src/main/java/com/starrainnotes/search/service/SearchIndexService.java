package com.starrainnotes.search.service;

import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.english.api.EnglishSearchTypes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchIndexService implements SearchIndexApi {
    public static final Set<String> TYPES = Stream.concat(
        Stream.of("TUTORIAL", "CHAPTER", "BLOG", "PORTFOLIO", "PROFILE"), EnglishSearchTypes.ALL.stream())
        .collect(Collectors.toUnmodifiableSet());
    private final SearchDocumentMapper mapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(SearchableDocument document) {
        if (document == null || !TYPES.contains(document.getContentType())
            || document.getContentId() == null || document.getContentId() <= 0
            || document.getDocumentKey() == null || document.getDocumentKey().length() > 120
            || !document.getDocumentKey().equals(document.getContentType() + ":" + document.getContentId())
            || document.getTitle() == null || document.getTitle().isBlank() || document.getTitle().length() > 255
            || document.getRoutePath() == null || !document.getRoutePath().startsWith("/")
            || document.getRoutePath().length() > 500) {
            throw new IllegalArgumentException("Invalid public search document");
        }
        if (document.getSummary() != null && document.getSummary().length() > 1000) {
            document.setSummary(document.getSummary().substring(0, 1000));
        }
        if (document.getSearchableText() == null) document.setSearchableText("");
        if (document.getSearchableText().length() > 100_000) {
            document.setSearchableText(document.getSearchableText().substring(0, 100_000));
        }
        if (document.getSourceUpdatedAt() == null) document.setSourceUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mapper.upsert(document);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String documentKey) {
        if (documentKey != null && !documentKey.isBlank()) mapper.remove(documentKey);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeByContent(String contentType, Long contentId) {
        if (TYPES.contains(contentType) && contentId != null && contentId > 0) mapper.removeByContent(contentType, contentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeChapters(String tutorialSlug) {
        if (tutorialSlug != null && tutorialSlug.matches("[a-zA-Z0-9_-]{1,120}")) {
            mapper.removeRoutePrefix("/tutorials/" + tutorialSlug + "/");
        }
    }
}

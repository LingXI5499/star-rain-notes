package com.starrainnotes.search.api;

import com.starrainnotes.search.api.dto.SearchableDocument;

public interface SearchIndexApi {
    void upsert(SearchableDocument document);
    void remove(String documentKey);
    void removeByContent(String contentType, Long contentId);
}

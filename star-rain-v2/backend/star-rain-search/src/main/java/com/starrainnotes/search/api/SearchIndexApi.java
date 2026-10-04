package com.starrainnotes.search.api;

public interface SearchIndexApi {
    void upsert(SearchableDocument document);
    void remove(String documentKey);
    void removeByContent(String contentType, Long contentId);
}

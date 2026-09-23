package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyCatalogRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import org.springframework.stereotype.Service;

/** Compatibility operations for the legacy global word memory counters. */
@Service
public class LegacyVocabularyMemoryService {
    private final VocabularyCatalogRepository catalog;
    private final VocabularyRepository repository;
    public LegacyVocabularyMemoryService(VocabularyCatalogRepository catalog,VocabularyRepository repository) {
        this.catalog=catalog; this.repository=repository;
    }
    @Deprecated
    public VocabularyWordView incrementLegacyMemory(long wordId) { return catalog.incrementMemory(wordId); }
    @Deprecated
    public VocabularyWordView setLegacyMemory(long wordId,SetMemoryRequest request) { return repository.setMemory(wordId,request); }
}

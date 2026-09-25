package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyCatalogRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import com.starrainnotes.english.vocabulary.service.VocabularyWordViewAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compatibility operations for the legacy global word memory counters. */
@Service
public class LegacyVocabularyMemoryService {
    private final VocabularyCatalogRepository catalog;
    private final VocabularyRepository repository;
    private final VocabularyWordViewAssembler assembler;
    public LegacyVocabularyMemoryService(VocabularyCatalogRepository catalog, VocabularyRepository repository,
                                         VocabularyWordViewAssembler assembler) {
        this.catalog = catalog;
        this.repository = repository;
        this.assembler = assembler;
    }
    @Deprecated
    @Transactional
    public VocabularyWordView incrementLegacyMemory(long wordId) {
        return assembler.toView(catalog.incrementMemory(wordId));
    }
    @Deprecated
    @Transactional
    public VocabularyWordView setLegacyMemory(long wordId, SetMemoryRequest request) {
        return repository.setMemory(wordId, request);
    }
}

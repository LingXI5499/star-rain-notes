package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.vocabulary.dto.*;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyCatalogRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VocabularyQueryService {
    private final VocabularyCatalogRepository catalog;
    private final VocabularyRepository repository;

    public VocabularyQueryService(VocabularyCatalogRepository catalog, VocabularyRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    public List<VocabularyLayerView> listLayers() { return catalog.listLayers(); }
    public VocabularyPageView listThemeWords(long themeId, int page, int size, boolean rememberedOnly) { return catalog.listThemeWords(themeId,page,size,rememberedOnly); }
    public VocabularyWordView getWord(long id) { return catalog.getWord(id); }
    public List<VocabularyWordView> getWords(List<Long> ids) { return catalog.getWords(ids); }
    public VocabularyPageView listWords(Long themeId,Integer layerOrder,String query,int page,int size) { return repository.listWords(themeId,layerOrder,query,page,size); }
}

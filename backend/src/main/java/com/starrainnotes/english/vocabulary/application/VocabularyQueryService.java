package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.domain.VocabularyLayer;
import com.starrainnotes.english.vocabulary.dto.VocabularyLayerView;
import com.starrainnotes.english.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyCatalogRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import com.starrainnotes.english.vocabulary.service.VocabularyWordViewAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VocabularyQueryService {
    private static final int MAX_PAGE_SIZE = 50;
    private final VocabularyCatalogRepository catalog;
    private final VocabularyRepository repository;
    private final VocabularyWordViewAssembler assembler;

    public VocabularyQueryService(VocabularyCatalogRepository catalog, VocabularyRepository repository,
                                  VocabularyWordViewAssembler assembler) {
        this.catalog = catalog;
        this.repository = repository;
        this.assembler = assembler;
    }

    public List<VocabularyLayerView> listLayers() {
        VocabularyCatalogRepository.ThemeCatalog catalogRows = catalog.themeCatalog();
        Map<String, List<VocabularyThemeView>> byLayer = new LinkedHashMap<>();
        for (VocabularyLayer layer : VocabularyLayer.values()) {
            byLayer.put(layer.label(), new ArrayList<>());
        }
        for (VocabularyTheme theme : catalogRows.themes()) {
            byLayer.computeIfAbsent(theme.getLayer(), key -> new ArrayList<>())
                    .add(new VocabularyThemeView(theme.getId(), theme.getName(),
                            catalogRows.wordCounts().getOrDefault(theme.getId(), 0)));
        }
        List<VocabularyLayerView> layers = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, List<VocabularyThemeView>> entry : byLayer.entrySet()) {
            layers.add(new VocabularyLayerView(entry.getKey(), ++order, entry.getValue()));
        }
        return layers;
    }

    public VocabularyPageView listThemeWords(long themeId, int page, int size, boolean rememberedOnly) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        VocabularyCatalogRepository.WordSlice slice = catalog.listThemeWords(themeId, safePage, safeSize, rememberedOnly);
        int totalPages = slice.total() == 0 ? 0 : (int) ((slice.total() + safeSize - 1) / safeSize);
        return new VocabularyPageView(assembler.toViews(slice.words()), slice.total(), safePage, safeSize, totalPages);
    }

    public VocabularyWordView getWord(long id) {
        return assembler.toView(catalog.getWord(id));
    }

    public List<VocabularyWordView> getWords(List<Long> ids) {
        return assembler.toViews(catalog.getWords(ids));
    }

    public VocabularyPageView listWords(Long themeId, Integer layerOrder, String query, int page, int size) {
        if (layerOrder != null && (layerOrder < 1 || layerOrder > 6)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOCABULARY_LAYER_INVALID",
                    "Invalid vocabulary layer", "Layer order must be between 1 and 6.");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return repository.listWords(themeId, layerOrder, query, safePage, safeSize);
    }
}

package com.starrainnotes.english.vocabulary.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.english.vocabulary.dto.CreateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.english.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.entity.VocabularyExample;
import com.starrainnotes.english.vocabulary.entity.VocabularyWord;
import com.starrainnotes.english.vocabulary.mapper.VocabularyWordMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyWordViewAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin commands and content-management queries for the vocabulary catalog. */
@Repository
public class VocabularyRepository {

    private static final int MAX_PAGE_SIZE = 50;
    private final VocabularyThemeRepository themeRepository;
    private final VocabularyWordMapper wordMapper;
    private final JdbcTemplate jdbc;
    private final VocabularyWordViewAssembler wordViewAssembler;

    public VocabularyRepository(VocabularyThemeRepository themeRepository,
                                  VocabularyWordMapper wordMapper,
                                  JdbcTemplate jdbc,
                                  VocabularyWordViewAssembler wordViewAssembler) {
        this.themeRepository = themeRepository;
        this.wordMapper = wordMapper;
        this.jdbc = jdbc;
        this.wordViewAssembler = wordViewAssembler;
    }

    @Transactional(readOnly = true)
    public VocabularyPageView listWords(Long themeId, Integer layerOrder, String query, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        if (themeId != null) {
            requireTheme(themeId);
            wrapper.eq("theme_id", themeId);
        } else if (layerOrder != null) {
            if (layerOrder < 1 || layerOrder > 6) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VOCABULARY_LAYER_INVALID",
                        "Invalid vocabulary layer", "Layer order must be between 1 and 6.");
            }
            List<Long> themeIds = themeRepository.idsForLayer(layerOrder);
            if (themeIds.isEmpty()) {
                return new VocabularyPageView(List.of(), 0, safePage, safeSize, 0);
            }
            wrapper.in("theme_id", themeIds);
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + escapeLike(query.trim()) + "%";
            wrapper.and(where -> where.like("word", pattern)
                    .or().like("translation", pattern)
                    .or().like("scene_meaning", pattern));
        }
        wrapper.orderByAsc("theme_id").orderByAsc("sort_order").orderByAsc("id");
        long total = wordMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<VocabularyWord> words = wordMapper.selectList(wrapper);
        List<VocabularyWordView> items = wordViewAssembler.toViews(words);
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new VocabularyPageView(items, total, safePage, safeSize, totalPages);
    }

    @Transactional
    public VocabularyWordView createWord(CreateVocabularyWordRequest request) {
        requireTheme(request.themeId());
        VocabularyWord word = new VocabularyWord();
        word.setThemeId(request.themeId());
        word.setPartOfSpeech(defaultEmpty(request.partOfSpeech()));
        word.setWord(request.word().trim());
        word.setPhoneticUs(clean(request.phoneticUs()));
        word.setPhoneticUk(clean(request.phoneticUk()));
        word.setTranslation(request.translation().trim());
        word.setSceneMeaning(clean(request.sceneMeaning()));
        word.setInflections(clean(request.inflections()));
        word.setExamples(List.of());
        word.setMemoryCount(0);
        word.setSortOrder(nextWordOrder(request.themeId()));
        wordMapper.insert(word);
        return wordViewAssembler.toView(word);
    }

    @Transactional
    public VocabularyWordView updateWord(long wordId, UpdateVocabularyWordRequest request) {
        VocabularyWord word = requireWord(wordId);
        if (request.themeId() != null) {
            requireTheme(request.themeId());
            if (!request.themeId().equals(word.getThemeId())) {
                word.setSortOrder(nextWordOrder(request.themeId()));
            }
            word.setThemeId(request.themeId());
        }
        if (request.partOfSpeech() != null) word.setPartOfSpeech(defaultEmpty(request.partOfSpeech()));
        if (request.word() != null) {
            if (request.word().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VOCABULARY_WORD_REQUIRED", "Word required",
                        "The English word cannot be blank.");
            }
            word.setWord(request.word().trim());
        }
        word.setTranslation(request.translation().trim());
        word.setSceneMeaning(clean(request.sceneMeaning()));
        word.setPhoneticUs(clean(request.phoneticUs()));
        word.setPhoneticUk(clean(request.phoneticUk()));
        word.setInflections(clean(request.inflections()));
        wordMapper.updateById(word);
        return wordViewAssembler.toView(word);
    }

    @Transactional
    public void deleteWord(long wordId) {
        requireWord(wordId);
        wordMapper.deleteById(wordId);
    }

    @Transactional
    public VocabularyWordView addExample(long wordId, AddExampleRequest request) {
        VocabularyWord word = requireWord(wordId);
        List<VocabularyExample> examples = new ArrayList<>(word.getExamples());
        examples.add(new VocabularyExample(request.sentence(), request.translation()));
        word.setExamples(examples);
        wordMapper.updateById(word);
        return wordViewAssembler.toView(word);
    }

    @Transactional
    public VocabularyWordView removeExample(long wordId, int index) {
        VocabularyWord word = requireWord(wordId);
        List<VocabularyExample> examples = new ArrayList<>(word.getExamples());
        if (index < 0 || index >= examples.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EXAMPLE_INDEX_OUT_OF_RANGE",
                    "Invalid example index", "Example index must be within the word's example list.");
        }
        examples.remove(index);
        word.setExamples(examples);
        wordMapper.updateById(word);
        return wordViewAssembler.toView(word);
    }

    @Transactional
    public VocabularyWordView setMemory(long wordId, SetMemoryRequest request) {
        VocabularyWord word = requireWord(wordId);
        word.setMemoryCount(request.memoryCount());
        wordMapper.updateById(word);
        return wordViewAssembler.toView(word);
    }

    private void requireTheme(long themeId) { themeRepository.requireTheme(themeId); }

    private VocabularyWord requireWord(long wordId) {
        VocabularyWord word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_WORD_NOT_FOUND",
                    "Vocabulary word not found", "No vocabulary word exists with id " + wordId + ".");
        }
        return word;
    }

    private int nextWordOrder(long themeId) {
        Integer value = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_word WHERE theme_id=?", Integer.class, themeId);
        return value == null ? 1 : value;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultEmpty(String value) {
        String cleaned = clean(value);
        return cleaned == null ? "" : cleaned;
    }
}

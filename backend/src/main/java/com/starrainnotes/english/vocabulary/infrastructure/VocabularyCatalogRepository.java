package com.starrainnotes.english.vocabulary.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyLayerView;
import com.starrainnotes.english.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.domain.VocabularyLayer;
import com.starrainnotes.english.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.english.vocabulary.entity.VocabularyWord;
import com.starrainnotes.english.vocabulary.mapper.VocabularyThemeMapper;
import com.starrainnotes.english.vocabulary.mapper.VocabularyWordMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyWordViewAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Theme-based vocabulary (approved spec change: theme + word tables).
 *
 * <p>Public catalog queries and the legacy public memory counter. Admin
 * write operations live in the vocabulary command use cases.</p>
 */
@Repository
@Transactional(readOnly = true)
public class VocabularyCatalogRepository {

    private static final int MAX_PAGE_SIZE = 50;

    private final VocabularyThemeMapper themeMapper;
    private final VocabularyWordMapper wordMapper;
    private final JdbcTemplate jdbc;
    private final VocabularyWordViewAssembler wordViewAssembler;

    public VocabularyCatalogRepository(VocabularyThemeMapper themeMapper,
                             VocabularyWordMapper wordMapper,
                             JdbcTemplate jdbc,
                             VocabularyWordViewAssembler wordViewAssembler) {
        this.themeMapper = themeMapper;
        this.wordMapper = wordMapper;
        this.jdbc = jdbc;
        this.wordViewAssembler = wordViewAssembler;
    }

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    /** Layer → themes (with word counts) for the category card page. */
    @Transactional(readOnly = true)
    public List<VocabularyLayerView> listLayers() {
        QueryWrapper<VocabularyTheme> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("layer_order").orderByAsc("sort_order");
        List<VocabularyTheme> themes = themeMapper.selectList(wrapper);

        Map<Long, Integer> counts = new HashMap<>();
        jdbc.query("SELECT theme_id, COUNT(*) AS c FROM vocabulary_word GROUP BY theme_id", rs -> {
            counts.put(rs.getLong("theme_id"), rs.getInt("c"));
        });

        Map<String, List<VocabularyThemeView>> byLayer = new LinkedHashMap<>();
        for (VocabularyLayer layer : VocabularyLayer.values()) byLayer.put(layer.label(), new ArrayList<>());
        for (VocabularyTheme theme : themes) {
            List<VocabularyThemeView> list = byLayer.computeIfAbsent(theme.getLayer(), k -> new ArrayList<>());
            list.add(new VocabularyThemeView(theme.getId(), theme.getName(),
                    counts.getOrDefault(theme.getId(), 0)));
        }
        List<VocabularyLayerView> layers = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, List<VocabularyThemeView>> entry : byLayer.entrySet()) {
            layers.add(new VocabularyLayerView(entry.getKey(), ++order, entry.getValue()));
        }
        return layers;
    }

    @Transactional(readOnly = true)
    public VocabularyPageView listThemeWords(long themeId, int page, int pageSize, boolean rememberedOnly) {
        requireTheme(themeId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        wrapper.eq("theme_id", themeId);
        if (rememberedOnly) {
            wrapper.gt("memory_count", 0);
        }
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        long total = wordMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<VocabularyWordView> items = wordViewAssembler.toViews(wordMapper.selectList(wrapper));
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new VocabularyPageView(items, total, safePage, safeSize, totalPages);
    }

    /** Personal memory +1: atomic increment and stamp the memory time. */
    @Transactional
    @Deprecated
    public VocabularyWordView incrementMemory(long wordId) {
        requireWord(wordId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UpdateWrapper<VocabularyWord> uw = new UpdateWrapper<>();
        uw.eq("id", wordId)
                .setSql("memory_count = memory_count + 1")
                .set("last_memory_at", now);
        wordMapper.update(null, uw);
        return wordViewAssembler.toView(requireWord(wordId));
    }

    @Transactional(readOnly = true)
    public VocabularyWordView getWord(long wordId) {
        return wordViewAssembler.toView(requireWord(wordId));
    }

    @Transactional(readOnly = true)
    public List<VocabularyWordView> getWords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> safeIds = ids.stream().filter(java.util.Objects::nonNull).distinct().limit(100).toList();
        if (safeIds.isEmpty()) return List.of();
        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        wrapper.in("id", safeIds);
        Map<Long, VocabularyWord> byId = new HashMap<>();
        for (VocabularyWord word : wordMapper.selectList(wrapper)) byId.put(word.getId(), word);
        List<VocabularyWord> ordered = safeIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return wordViewAssembler.toViews(ordered);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private VocabularyTheme requireTheme(long themeId) {
        VocabularyTheme theme = themeMapper.selectById(themeId);
        if (theme == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_THEME_NOT_FOUND",
                    "Vocabulary theme not found", "No vocabulary theme exists with id " + themeId + ".");
        }
        return theme;
    }

    private VocabularyWord requireWord(long wordId) {
        VocabularyWord word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_WORD_NOT_FOUND",
                    "Vocabulary word not found", "No vocabulary word exists with id " + wordId + ".");
        }
        return word;
    }

}

package com.starrainnotes.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.VocabularyLayerView;
import com.starrainnotes.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.entity.VocabularyExample;
import com.starrainnotes.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.vocabulary.entity.VocabularyWord;
import com.starrainnotes.vocabulary.mapper.VocabularyThemeMapper;
import com.starrainnotes.vocabulary.mapper.VocabularyWordMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Theme-based vocabulary (approved spec change: theme + word tables).
 *
 * <p>Public: layer/theme listing, paged words per theme, and the personal
 * memory +1 (single-admin personal site — the owner reviews the public
 * cards while not necessarily logged in). Admin: word correction, example
 * sentence management and memory-count correction.</p>
 */
@Service
public class VocabularyService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 50;

    private final VocabularyThemeMapper themeMapper;
    private final VocabularyWordMapper wordMapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public VocabularyService(VocabularyThemeMapper themeMapper,
                             VocabularyWordMapper wordMapper,
                             JdbcTemplate jdbc,
                             SiteSettingsTimezone timezone) {
        this.themeMapper = themeMapper;
        this.wordMapper = wordMapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    /** Layer → themes (with word counts) for the category card page. */
    public List<VocabularyLayerView> listLayers() {
        QueryWrapper<VocabularyTheme> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("layer_order").orderByAsc("sort_order");
        List<VocabularyTheme> themes = themeMapper.selectList(wrapper);

        Map<Long, Integer> counts = new HashMap<>();
        jdbc.query("SELECT theme_id, COUNT(*) AS c FROM vocabulary_word GROUP BY theme_id", rs -> {
            counts.put(rs.getLong("theme_id"), rs.getInt("c"));
        });

        Map<String, List<VocabularyThemeView>> byLayer = new LinkedHashMap<>();
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
        List<VocabularyWordView> items = wordMapper.selectList(wrapper).stream().map(this::toView).toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new VocabularyPageView(items, total, safePage, safeSize, totalPages);
    }

    /** Personal memory +1: atomic increment and stamp the memory time. */
    @Transactional
    public VocabularyWordView incrementMemory(long wordId) {
        requireWord(wordId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UpdateWrapper<VocabularyWord> uw = new UpdateWrapper<>();
        uw.eq("id", wordId)
                .setSql("memory_count = memory_count + 1")
                .set("last_memory_at", now);
        wordMapper.update(null, uw);
        return toView(requireWord(wordId));
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    public VocabularyPageView adminListWords(Long themeId, String query, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        if (themeId != null) {
            requireTheme(themeId);
            wrapper.eq("theme_id", themeId);
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + escapeLike(query.trim()) + "%";
            wrapper.and(w -> w.like("word", pattern).or().like("translation", pattern));
        }
        wrapper.orderByAsc("theme_id").orderByAsc("sort_order").orderByAsc("id");
        long total = wordMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<VocabularyWordView> items = wordMapper.selectList(wrapper).stream().map(this::toView).toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new VocabularyPageView(items, total, safePage, safeSize, totalPages);
    }

    @Transactional
    public VocabularyWordView updateWord(long wordId, UpdateVocabularyWordRequest request) {
        VocabularyWord word = requireWord(wordId);
        word.setTranslation(request.translation());
        word.setPhoneticUs(request.phoneticUs());
        word.setInflections(request.inflections());
        wordMapper.updateById(word);
        return toView(word);
    }

    @Transactional
    public VocabularyWordView addExample(long wordId, AddExampleRequest request) {
        VocabularyWord word = requireWord(wordId);
        List<VocabularyExample> examples = new ArrayList<>(word.getExamples());
        examples.add(new VocabularyExample(request.sentence(), request.translation()));
        word.setExamples(examples);
        wordMapper.updateById(word);
        return toView(word);
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
        return toView(word);
    }

    @Transactional
    public VocabularyWordView setMemory(long wordId, SetMemoryRequest request) {
        requireWord(wordId);
        // A count correction is not a memory event, so last_memory_at stays.
        UpdateWrapper<VocabularyWord> uw = new UpdateWrapper<>();
        uw.eq("id", wordId).set("memory_count", request.memoryCount());
        wordMapper.update(null, uw);
        return toView(requireWord(wordId));
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

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private VocabularyWordView toView(VocabularyWord word) {
        return new VocabularyWordView(
                word.getId(),
                word.getThemeId(),
                word.getPartOfSpeech(),
                word.getWord(),
                word.getPhoneticUs(),
                word.getTranslation(),
                word.getInflections(),
                word.getExamples() == null ? List.of() : word.getExamples(),
                word.getMemoryCount() == null ? 0 : word.getMemoryCount(),
                word.getLastMemoryAt() == null ? null
                        : timezone.atSite(word.getLastMemoryAt()).format(ISO_OFFSET));
    }
}

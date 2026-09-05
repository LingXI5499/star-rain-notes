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
import com.starrainnotes.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.vocabulary.dto.VocabularyFamilyView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        List<VocabularyWordView> items = toViews(wordMapper.selectList(wrapper));
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

    public VocabularyWordView getWord(long wordId) {
        return toView(requireWord(wordId));
    }

    public List<VocabularyWordView> getWords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> safeIds = ids.stream().filter(java.util.Objects::nonNull).distinct().limit(100).toList();
        if (safeIds.isEmpty()) return List.of();
        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        wrapper.in("id", safeIds);
        Map<Long, VocabularyWord> byId = new HashMap<>();
        for (VocabularyWord word : wordMapper.selectList(wrapper)) byId.put(word.getId(), word);
        List<VocabularyWord> ordered = safeIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return toViews(ordered);
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
        List<VocabularyWordView> items = toViews(wordMapper.selectList(wrapper));
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new VocabularyPageView(items, total, safePage, safeSize, totalPages);
    }

    @Transactional
    public VocabularyWordView updateWord(long wordId, UpdateVocabularyWordRequest request) {
        VocabularyWord word = requireWord(wordId);
        word.setTranslation(request.translation());
        word.setPhoneticUs(request.phoneticUs());
        word.setPhoneticUk(request.phoneticUk());
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

    @Transactional
    public VocabularyAudioView addAudio(long wordId, VocabularyAudioRequest request) {
        requireWord(wordId);
        Map<String, Object> media;
        try {
            media = jdbc.queryForMap("SELECT id,asset_type,public_url FROM media_asset WHERE id=?", request.mediaAssetId());
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found",
                    "The selected media asset does not exist.");
        }
        if (!"AUDIO".equals(String.valueOf(media.get("asset_type")))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VOCABULARY_AUDIO_REQUIRED", "Audio required",
                    "The selected media asset must be an audio file.");
        }
        if (request.primary()) {
            jdbc.update("UPDATE vocabulary_word_audio SET is_primary=FALSE WHERE word_id=? AND accent=?",
                    wordId, request.accent());
        }
        jdbc.update("""
                INSERT INTO vocabulary_word_audio
                (word_id,accent,media_asset_id,provider,source_url,license_note,is_primary)
                VALUES (?,?,?,?,?,?,?)
                """, wordId, request.accent(), request.mediaAssetId(),
                request.provider() == null ? "UPLOADED" : request.provider(), clean(request.sourceUrl()),
                request.licenseNote().trim(), request.primary());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return audio(id);
    }

    @Transactional
    public void deleteAudio(long wordId, long audioId) {
        requireWord(wordId);
        if (jdbc.update("DELETE FROM vocabulary_word_audio WHERE id=? AND word_id=?", audioId, wordId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_AUDIO_NOT_FOUND", "Audio not found",
                    "The pronunciation audio relation does not exist.");
        }
    }

    @Transactional
    public VocabularyAudioView setPrimaryAudio(long wordId, long audioId) {
        requireWord(wordId);
        String accent;
        try {
            accent = jdbc.queryForObject(
                    "SELECT accent FROM vocabulary_word_audio WHERE id=? AND word_id=?", String.class, audioId, wordId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_AUDIO_NOT_FOUND", "Audio not found",
                    "The pronunciation audio relation does not exist.");
        }
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=FALSE WHERE word_id=? AND accent=?", wordId, accent);
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=TRUE WHERE id=?", audioId);
        return audio(audioId);
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
        return toViews(List.of(word)).getFirst();
    }

    private List<VocabularyWordView> toViews(List<VocabularyWord> words) {
        if (words == null || words.isEmpty()) return List.of();
        List<Long> ids = words.stream().map(VocabularyWord::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, List<VocabularyAudioView>> audioByWord = new HashMap<>();
        jdbc.query("""
                SELECT a.id,a.word_id,a.accent,a.media_asset_id,m.public_url,a.provider,a.source_url,
                       a.license_note,a.is_primary
                FROM vocabulary_word_audio a JOIN media_asset m ON m.id=a.media_asset_id
                WHERE a.word_id IN (%s)
                ORDER BY a.word_id,a.is_primary DESC,a.accent,a.id
                """.formatted(placeholders), rs -> {
            audioByWord.computeIfAbsent(rs.getLong("word_id"), ignored -> new ArrayList<>()).add(new VocabularyAudioView(
                    rs.getLong("id"), rs.getString("accent"), rs.getLong("media_asset_id"),
                    rs.getString("public_url"), rs.getString("provider"), rs.getString("source_url"),
                    rs.getString("license_note"), rs.getBoolean("is_primary")));
        }, ids.toArray());
        Map<Long, List<VocabularyFamilyView>> familyByWord = new HashMap<>();
        jdbc.query("""
                SELECT l.word_id,f.id,f.head_word,f.slug
                FROM vocabulary_word_family_link l JOIN vocabulary_word_family f ON f.id=l.family_id
                WHERE l.word_id IN (%s) ORDER BY l.word_id,f.id
                """.formatted(placeholders), rs -> {
            familyByWord.computeIfAbsent(rs.getLong("word_id"), ignored -> new ArrayList<>()).add(
                    new VocabularyFamilyView(rs.getLong("id"), rs.getString("head_word"), rs.getString("slug")));
        }, ids.toArray());
        return words.stream().map(word -> new VocabularyWordView(
                word.getId(),
                word.getThemeId(),
                word.getPartOfSpeech(),
                word.getWord(),
                word.getPhoneticUs(),
                word.getPhoneticUk(),
                word.getTranslation(),
                word.getInflections(),
                word.getExamples() == null ? List.of() : word.getExamples(),
                word.getMemoryCount() == null ? 0 : word.getMemoryCount(),
                word.getLastMemoryAt() == null ? null
                        : timezone.atSite(word.getLastMemoryAt()).format(ISO_OFFSET),
                audioByWord.getOrDefault(word.getId(), List.of()),
                familyByWord.getOrDefault(word.getId(), List.of()))).toList();
    }

    private VocabularyAudioView audio(Long audioId) {
        try {
            return jdbc.queryForObject("""
                    SELECT a.id,a.accent,a.media_asset_id,m.public_url,a.provider,a.source_url,
                           a.license_note,a.is_primary
                    FROM vocabulary_word_audio a JOIN media_asset m ON m.id=a.media_asset_id WHERE a.id=?
                    """, (rs, rowNum) -> new VocabularyAudioView(rs.getLong("id"), rs.getString("accent"),
                    rs.getLong("media_asset_id"), rs.getString("public_url"), rs.getString("provider"),
                    rs.getString("source_url"), rs.getString("license_note"), rs.getBoolean("is_primary")), audioId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_AUDIO_NOT_FOUND", "Audio not found",
                    "The pronunciation audio relation does not exist.");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

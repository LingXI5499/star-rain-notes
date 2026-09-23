package com.starrainnotes.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.vocabulary.dto.CreateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.vocabulary.dto.VocabularyThemeRequest;
import com.starrainnotes.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.domain.VocabularyLayer;
import com.starrainnotes.vocabulary.entity.VocabularyExample;
import com.starrainnotes.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.vocabulary.entity.VocabularyWord;
import com.starrainnotes.vocabulary.mapper.VocabularyThemeMapper;
import com.starrainnotes.vocabulary.mapper.VocabularyWordMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin commands and content-management queries for the vocabulary catalog. */
@Service
public class VocabularyAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private final VocabularyThemeMapper themeMapper;
    private final VocabularyWordMapper wordMapper;
    private final JdbcTemplate jdbc;
    private final VocabularyWordViewAssembler wordViewAssembler;

    public VocabularyAdminService(VocabularyThemeMapper themeMapper,
                                  VocabularyWordMapper wordMapper,
                                  JdbcTemplate jdbc,
                                  VocabularyWordViewAssembler wordViewAssembler) {
        this.themeMapper = themeMapper;
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
            List<Long> themeIds = themeMapper.selectList(new QueryWrapper<VocabularyTheme>()
                            .eq("layer_order", layerOrder))
                    .stream().map(VocabularyTheme::getId).toList();
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
    public VocabularyThemeView createTheme(VocabularyThemeRequest request) {
        VocabularyTheme theme = new VocabularyTheme();
        theme.setLayer(VocabularyLayer.fromOrder(request.layerOrder()).label());
        theme.setLayerOrder(request.layerOrder());
        theme.setName(request.name().trim());
        theme.setSortOrder(request.sortOrder() == null ? nextThemeOrder() : request.sortOrder());
        themeMapper.insert(theme);
        return new VocabularyThemeView(theme.getId(), theme.getName(), 0);
    }

    @Transactional
    public VocabularyThemeView updateTheme(long themeId, VocabularyThemeRequest request) {
        VocabularyTheme theme = requireTheme(themeId);
        theme.setLayer(VocabularyLayer.fromOrder(request.layerOrder()).label());
        theme.setLayerOrder(request.layerOrder());
        theme.setName(request.name().trim());
        if (request.sortOrder() != null) theme.setSortOrder(request.sortOrder());
        themeMapper.updateById(theme);
        long count = wordMapper.selectCount(new QueryWrapper<VocabularyWord>().eq("theme_id", themeId));
        return new VocabularyThemeView(theme.getId(), theme.getName(), count);
    }

    @Transactional
    public void deleteTheme(long themeId) {
        requireTheme(themeId);
        long count = wordMapper.selectCount(new QueryWrapper<VocabularyWord>().eq("theme_id", themeId));
        if (count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "VOCABULARY_THEME_NOT_EMPTY", "Theme is not empty",
                    "Move or delete the theme's vocabulary words before deleting this category.");
        }
        themeMapper.deleteById(themeId);
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

    @Transactional
    public VocabularyAudioView addAudio(long wordId, VocabularyAudioRequest request) {
        requireWord(wordId);
        Map<String, Object> media;
        try {
            media = jdbc.queryForMap("SELECT id,asset_type,public_url FROM media_asset WHERE id=?", request.mediaAssetId());
        } catch (EmptyResultDataAccessException ex) {
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
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_AUDIO_NOT_FOUND", "Audio not found",
                    "The pronunciation audio relation does not exist.");
        }
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=FALSE WHERE word_id=? AND accent=?", wordId, accent);
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=TRUE WHERE id=?", audioId);
        return audio(audioId);
    }

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

    private int nextThemeOrder() {
        Integer value = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_theme", Integer.class);
        return value == null ? 1 : value;
    }

    private int nextWordOrder(long themeId) {
        Integer value = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_word WHERE theme_id=?", Integer.class, themeId);
        return value == null ? 1 : value;
    }

    private VocabularyAudioView audio(long audioId) {
        try {
            return jdbc.queryForObject("""
                    SELECT a.id,a.accent,a.media_asset_id,m.public_url,a.provider,a.source_url,
                           a.license_note,a.is_primary
                    FROM vocabulary_word_audio a JOIN media_asset m ON m.id=a.media_asset_id WHERE a.id=?
                    """, (rs, rowNum) -> new VocabularyAudioView(rs.getLong("id"), rs.getString("accent"),
                    rs.getLong("media_asset_id"), rs.getString("public_url"), rs.getString("provider"),
                    rs.getString("source_url"), rs.getString("license_note"), rs.getBoolean("is_primary")), audioId);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_AUDIO_NOT_FOUND", "Audio not found",
                    "The pronunciation audio relation does not exist.");
        }
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

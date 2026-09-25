package com.starrainnotes.english.vocabulary.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.english.vocabulary.entity.VocabularyWord;
import com.starrainnotes.english.vocabulary.mapper.VocabularyThemeMapper;
import com.starrainnotes.english.vocabulary.mapper.VocabularyWordMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
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

    private final VocabularyThemeMapper themeMapper;
    private final VocabularyWordMapper wordMapper;
    private final JdbcTemplate jdbc;

    public VocabularyCatalogRepository(VocabularyThemeMapper themeMapper,
                             VocabularyWordMapper wordMapper,
                             JdbcTemplate jdbc) {
        this.themeMapper = themeMapper;
        this.wordMapper = wordMapper;
        this.jdbc = jdbc;
    }

    public record ThemeCatalog(List<VocabularyTheme> themes, Map<Long, Integer> wordCounts) {}

    public record WordSlice(List<VocabularyWord> words, long total, int page, int size) {}

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    /** Layer → themes (with word counts) for the category card page. */
    public ThemeCatalog themeCatalog() {
        QueryWrapper<VocabularyTheme> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("layer_order").orderByAsc("sort_order");
        List<VocabularyTheme> themes = themeMapper.selectList(wrapper);
        Map<Long, Integer> counts = new HashMap<>();
        jdbc.query("SELECT theme_id, COUNT(*) AS c FROM vocabulary_word GROUP BY theme_id", rs -> {
            counts.put(rs.getLong("theme_id"), rs.getInt("c"));
        });
        return new ThemeCatalog(themes, counts);
    }

    public WordSlice listThemeWords(long themeId, int page, int pageSize, boolean rememberedOnly) {
        requireTheme(themeId);
        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        wrapper.eq("theme_id", themeId);
        if (rememberedOnly) {
            wrapper.gt("memory_count", 0);
        }
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        long total = wordMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize));
        return new WordSlice(wordMapper.selectList(wrapper), total, page, pageSize);
    }

    /** Personal memory +1: atomic increment and stamp the memory time. */
    @Deprecated
    public VocabularyWord incrementMemory(long wordId) {
        requireWord(wordId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UpdateWrapper<VocabularyWord> uw = new UpdateWrapper<>();
        uw.eq("id", wordId)
                .setSql("memory_count = memory_count + 1")
                .set("last_memory_at", now);
        wordMapper.update(null, uw);
        return requireWord(wordId);
    }

    public VocabularyWord getWord(long wordId) {
        return requireWord(wordId);
    }

    public List<VocabularyWord> getWords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> safeIds = ids.stream().filter(java.util.Objects::nonNull).distinct().limit(100).toList();
        if (safeIds.isEmpty()) return List.of();
        QueryWrapper<VocabularyWord> wrapper = new QueryWrapper<>();
        wrapper.in("id", safeIds);
        Map<Long, VocabularyWord> byId = new HashMap<>();
        for (VocabularyWord word : wordMapper.selectList(wrapper)) byId.put(word.getId(), word);
        return safeIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
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

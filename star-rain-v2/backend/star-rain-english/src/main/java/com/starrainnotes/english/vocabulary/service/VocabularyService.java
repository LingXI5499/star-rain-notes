package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.ThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.WordRequest;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VocabularyService {
    private static final int MAX_PAGE_SIZE = 100;
    private final VocabularyMapper mapper;

    @Transactional(readOnly = true)
    public List<Theme> themes() { return mapper.themes(); }

    @Transactional(readOnly = true)
    public Page<Word> words(String themeId, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        Long selectedTheme = themeId == null || themeId.isBlank() ? null : id(themeId);
        String term = search == null ? "" : search.trim();
        long total = mapper.countWords(selectedTheme, term);
        List<Word> items = total == 0 ? List.of()
                : mapper.words(selectedTheme, term, (long) (safePage - 1) * safeSize, safeSize);
        return new Page<>(items, total, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public Word word(String wordId) {
        Word word = mapper.word(id(wordId));
        if (word == null) throw new ApiException("ENGLISH_WORD_NOT_FOUND", "单词不存在", 404);
        return word;
    }

    @Transactional
    public Theme createTheme(ThemeRequest request) {
        validateTheme(request);
        Theme theme = new Theme();
        theme.setLayer(request.layer().trim());
        theme.setLayerOrder(number(request.layerOrder()));
        theme.setName(request.name().trim());
        theme.setSortOrder(number(request.sortOrder()));
        mapper.insertTheme(theme);
        return requiredTheme(theme.getId());
    }

    @Transactional
    public Theme updateTheme(String themeId, ThemeRequest request) {
        validateTheme(request);
        Theme theme = new Theme();
        theme.setId(id(themeId));
        theme.setLayer(request.layer().trim());
        theme.setLayerOrder(number(request.layerOrder()));
        theme.setName(request.name().trim());
        theme.setSortOrder(number(request.sortOrder()));
        if (mapper.updateTheme(theme) == 0)
            throw new ApiException("ENGLISH_THEME_NOT_FOUND", "词汇主题不存在", 404);
        return requiredTheme(theme.getId());
    }

    @Transactional
    public void deleteTheme(String themeId) {
        if (mapper.deleteTheme(id(themeId)) == 0)
            throw new ApiException("ENGLISH_THEME_NOT_FOUND", "词汇主题不存在", 404);
    }

    @Transactional
    public Word createWord(WordRequest request) {
        validateWord(request);
        Word word = fromRequest(request);
        mapper.insertWord(word);
        return word(String.valueOf(word.getId()));
    }

    @Transactional
    public Word updateWord(String wordId, WordRequest request) {
        validateWord(request);
        Word word = fromRequest(request);
        word.setId(id(wordId));
        if (mapper.updateWord(word) == 0)
            throw new ApiException("ENGLISH_WORD_NOT_FOUND", "单词不存在", 404);
        return word(wordId);
    }

    @Transactional
    public void deleteWord(String wordId) {
        if (mapper.deleteWord(id(wordId)) == 0)
            throw new ApiException("ENGLISH_WORD_NOT_FOUND", "单词不存在", 404);
    }

    private Theme requiredTheme(Long themeId) {
        Theme theme = mapper.theme(themeId);
        if (theme == null) throw new ApiException("ENGLISH_THEME_NOT_FOUND", "词汇主题不存在", 404);
        return theme;
    }

    private Word fromRequest(WordRequest request) {
        Word word = new Word();
        word.setThemeId(id(request.themeId()));
        word.setWord(request.word().trim());
        word.setPartOfSpeech(empty(request.partOfSpeech()));
        word.setPhoneticUs(empty(request.phoneticUs()));
        word.setPhoneticUk(empty(request.phoneticUk()));
        word.setTranslation(request.translation().trim());
        word.setExamples(request.examples() == null || request.examples().isBlank() ? "[]" : request.examples());
        word.setSortOrder(number(request.sortOrder()));
        return word;
    }

    private void validateTheme(ThemeRequest request) {
        if (request == null || request.layer() == null || request.layer().isBlank()
                || request.name() == null || request.name().isBlank())
            throw new ApiException("ENGLISH_THEME_INVALID", "请填写词层和主题名称", 400);
        if (request.layer().length() > 100 || request.name().length() > 200)
            throw new ApiException("ENGLISH_THEME_INVALID", "词层或主题名称过长", 400);
    }

    private void validateWord(WordRequest request) {
        if (request == null || request.themeId() == null || request.word() == null
                || request.word().isBlank() || request.translation() == null || request.translation().isBlank())
            throw new ApiException("ENGLISH_WORD_INVALID", "请填写主题、单词和释义", 400);
        id(request.themeId());
        if (request.word().length() > 200 || request.translation().length() > 1000)
            throw new ApiException("ENGLISH_WORD_INVALID", "单词或释义过长", 400);
    }

    private long id(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) {
            throw new ApiException("ENGLISH_ID_INVALID", "无效的内容编号", 400);
        }
    }

    private int number(Integer value) { return value == null ? 0 : value; }
    private String empty(String value) { return value == null ? "" : value.trim(); }
}

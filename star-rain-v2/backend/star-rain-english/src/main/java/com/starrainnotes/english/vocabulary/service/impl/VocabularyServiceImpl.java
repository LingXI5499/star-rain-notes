package com.starrainnotes.english.vocabulary.service.impl;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.ThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.WordRequest;
import com.starrainnotes.english.vocabulary.exception.VocabularyContentInvalidException;
import com.starrainnotes.english.vocabulary.exception.VocabularyThemeNotFoundException;
import com.starrainnotes.english.vocabulary.exception.VocabularyWordNotFoundException;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import com.starrainnotes.english.vocabulary.vo.VocabularyWordAudioVO;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VocabularyServiceImpl implements VocabularyService {
    private static final int MAX_PAGE_SIZE = 100;

    private final VocabularyMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<Theme> themes() {
        return mapper.themes();
    }

    /*
     * 分页取词。
     *
     * page 从 1 开始，小于 1 一律按 1；size 钳在 1..100，避免一次拉全库。
     * 返回里同时给出 totalPages，前端不必自己再算一遍（V1 的 /themes/{id}/words 也返回它）。
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Word> words(String themeId, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        Long selectedTheme = themeId == null || themeId.isBlank() ? null : id(themeId);
        String term = search == null ? "" : search.trim();
        long total = mapper.countWords(selectedTheme, term);
        List<Word> items = total == 0
                ? List.of()
                : mapper.words(selectedTheme, term, (long) (safePage - 1) * safeSize, safeSize);
        attachAudios(items);
        return Page.<Word>builder()
                .items(items)
                .total(total)
                .page(safePage)
                .size(safeSize)
                .totalPages((int) ((total + safeSize - 1) / safeSize))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Word word(String wordId) {
        return requiredWord(id(wordId));
    }

    /*
     * 按编号批量取词。调用方给出的顺序不保留：结果按主题内展示顺序（sort_order, id）返回，
     * 与主题页一致；需要特定顺序（如复习队列的到期优先）时由调用方按编号自行重排。
     * maxWords 由调用方给出，公开批量接口与复习队列的数量级不同。
     */
    @Override
    @Transactional(readOnly = true)
    public List<Word> wordsByIds(List<Long> wordIds, int maxWords) {
        if (wordIds == null || wordIds.isEmpty() || maxWords <= 0) {
            return List.of();
        }
        List<Long> ids = wordIds.stream().filter(Objects::nonNull).distinct()
                .limit(maxWords).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Word> words = new ArrayList<>(mapper.wordsByIds(ids));
        attachAudios(words);
        return words;
    }

    @Override
    @Transactional
    public Theme createTheme(ThemeRequest request) {
        validateTheme(request);
        Theme theme = new Theme();
        theme.setLayer(request.getLayer().trim());
        theme.setLayerOrder(number(request.getLayerOrder()));
        theme.setName(request.getName().trim());
        theme.setSortOrder(number(request.getSortOrder()));
        mapper.insertTheme(theme);
        changed("ENGLISH_VOCABULARY_THEME", theme.getId());
        return requiredTheme(theme.getId());
    }

    @Override
    @Transactional
    public Theme updateTheme(String themeId, ThemeRequest request) {
        validateTheme(request);
        Theme theme = new Theme();
        theme.setId(id(themeId));
        theme.setLayer(request.getLayer().trim());
        theme.setLayerOrder(number(request.getLayerOrder()));
        theme.setName(request.getName().trim());
        theme.setSortOrder(number(request.getSortOrder()));
        if (mapper.updateTheme(theme) == 0) {
            throw new VocabularyThemeNotFoundException();
        }
        changed("ENGLISH_VOCABULARY_THEME", theme.getId());
        return requiredTheme(theme.getId());
    }

    @Override
    @Transactional
    public void deleteTheme(String themeId) {
        if (mapper.deleteTheme(id(themeId)) == 0) {
            throw new VocabularyThemeNotFoundException();
        }
        changed("ENGLISH_VOCABULARY_THEME", id(themeId));
        changed("ENGLISH_VOCABULARY_WORD", null);
    }

    @Override
    @Transactional
    public Word createWord(WordRequest request) {
        validateWord(request);
        Word word = fromRequest(request);
        mapper.insertWord(word);
        changed("ENGLISH_VOCABULARY_WORD", word.getId());
        return requiredWord(word.getId());
    }

    @Override
    @Transactional
    public Word updateWord(String wordId, WordRequest request) {
        validateWord(request);
        Word word = fromRequest(request);
        word.setId(id(wordId));
        if (mapper.updateWord(word) == 0) {
            throw new VocabularyWordNotFoundException();
        }
        changed("ENGLISH_VOCABULARY_WORD", word.getId());
        return requiredWord(word.getId());
    }

    @Override
    @Transactional
    public void deleteWord(String wordId) {
        if (mapper.deleteWord(id(wordId)) == 0) {
            throw new VocabularyWordNotFoundException();
        }
        changed("ENGLISH_VOCABULARY_WORD", id(wordId));
    }

    private void changed(String type, Long id) {
        events.publishEvent(new EnglishSearchContentChangedEvent(type, id));
    }

    private Word requiredWord(long wordId) {
        Word word = mapper.word(wordId);
        if (word == null) {
            throw new VocabularyWordNotFoundException();
        }
        attachAudios(List.of(word));
        return word;
    }

    private Theme requiredTheme(Long themeId) {
        Theme theme = mapper.theme(themeId);
        if (theme == null) {
            throw new VocabularyThemeNotFoundException();
        }
        return theme;
    }

    /*
     * 批量补齐授权发音。逐词查询会变成 N+1，因此一次取回所有词的全部音频再分组挂载；
     * 只有存在媒体资产的音频才给 publicUrl，前端据此决定是否优先播放。
     */
    private void attachAudios(List<Word> words) {
        if (words == null || words.isEmpty()) {
            return;
        }
        List<Long> ids = words.stream().map(Word::getId).filter(Objects::nonNull).toList();
        Map<Long, List<VocabularyWordAudioVO>> grouped = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (VocabularyWordAudioVO audio : mapper.audios(ids)) {
                audio.setPublicUrl(publicUrl(audio.getMediaAssetId()));
                grouped.computeIfAbsent(audio.getWordId(), key -> new ArrayList<>()).add(audio);
            }
        }
        for (Word word : words) {
            word.setAudios(grouped.getOrDefault(word.getId(), List.of()));
        }
    }

    private String publicUrl(Long mediaAssetId) {
        return mediaAssetId == null ? null : "/api/media/assets/" + mediaAssetId + "/content";
    }

    private Word fromRequest(WordRequest request) {
        Word word = new Word();
        word.setThemeId(id(request.getThemeId()));
        word.setWord(request.getWord().trim());
        word.setPartOfSpeech(empty(request.getPartOfSpeech()));
        word.setPhoneticUs(empty(request.getPhoneticUs()));
        word.setPhoneticUk(empty(request.getPhoneticUk()));
        word.setTranslation(request.getTranslation().trim());
        word.setSceneMeaning(trimToNull(request.getSceneMeaning()));
        word.setInflections(trimToNull(request.getInflections()));
        word.setExamples(request.getExamples() == null || request.getExamples().isBlank() ? "[]" : request.getExamples());
        word.setSortOrder(number(request.getSortOrder()));
        return word;
    }

    private void validateTheme(ThemeRequest request) {
        if (request == null || isBlank(request.getLayer()) || isBlank(request.getName())) {
            throw new VocabularyContentInvalidException("请填写词层和主题名称");
        }
        if (request.getLayer().length() > 100 || request.getName().length() > 200) {
            throw new VocabularyContentInvalidException("词层或主题名称过长");
        }
    }

    private void validateWord(WordRequest request) {
        if (request == null || isBlank(request.getThemeId()) || isBlank(request.getWord())
                || isBlank(request.getTranslation())) {
            throw new VocabularyContentInvalidException("请填写主题、单词和释义");
        }
        if (request.getWord().length() > 200 || request.getTranslation().length() > 1000) {
            throw new VocabularyContentInvalidException("单词或释义过长");
        }
        if (request.getSceneMeaning() != null && request.getSceneMeaning().length() > 1000) {
            throw new VocabularyContentInvalidException("本主题用法不能超过 1000 个字符");
        }
        if (request.getInflections() != null && request.getInflections().length() > 1000) {
            throw new VocabularyContentInvalidException("词形变化不能超过 1000 个字符");
        }
        id(request.getThemeId());
    }

    private long id(String value) {
        if (isBlank(value)) {
            throw new VocabularyContentInvalidException("无效的内容编号");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new VocabularyContentInvalidException("无效的内容编号");
        }
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String empty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

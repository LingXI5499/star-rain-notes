/*
 * VocabularyServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：Page、Theme、ThemeRequest、Word、WordRequest、VocabularyContentInvalidException、VocabularyThemeNotFoundException。
 * 阅读入口：themes（themes处理入口）；words（words处理入口）；word（单词处理入口）；wordsByIds（按标识集合读取words）。
 */

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

/**
 * 词汇业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class VocabularyServiceImpl implements VocabularyService {
    /**
     * 大小。
     * 初始值为 <code>100</code>。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyMapper</code>。
     */
    private final VocabularyMapper mapper;
    /**
     * 事件集合。
     */
    private final ApplicationEventPublisher events;

    /**
     * themes处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.themes</code>。
     * @return 本次处理得到的条目集合
     */
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
    /**
     * words处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.countWords</code>、<code>mapper.words</code>。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page&lt;Word&gt;</code> 的处理结果
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

    /**
     * 单词处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>requiredWord</code>、<code>id</code>。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
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
    /**
     * 按标识集合读取words。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.wordsByIds</code>。
     *
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param maxWords 最大Words；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
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

    /**
     * 创建主题。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insertTheme</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ThemeRequest</code>
     * @return 类型为 <code>Theme</code> 的处理结果
     */
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

    /**
     * 更新主题。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateTheme</code>。
     * 实现中的显式失败类型包括 <code>VocabularyThemeNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ThemeRequest</code>
     * @return 类型为 <code>Theme</code> 的处理结果
     */
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

    /**
     * 删除主题。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteTheme</code>。
     * 实现中的显式失败类型包括 <code>VocabularyThemeNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void deleteTheme(String themeId) {
        if (mapper.deleteTheme(id(themeId)) == 0) {
            throw new VocabularyThemeNotFoundException();
        }
        changed("ENGLISH_VOCABULARY_THEME", id(themeId));
        changed("ENGLISH_VOCABULARY_WORD", null);
    }

    /**
     * 创建单词。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insertWord</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    @Override
    @Transactional
    public Word createWord(WordRequest request) {
        validateWord(request);
        Word word = fromRequest(request);
        mapper.insertWord(word);
        mapper.initializeLibraryMapping(word);
        changed("ENGLISH_VOCABULARY_WORD", word.getId());
        return requiredWord(word.getId());
    }

    /**
     * 更新单词。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateWord</code>。
     * 实现中的显式失败类型包括 <code>VocabularyWordNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
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

    /**
     * 删除单词。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteWord</code>。
     * 实现中的显式失败类型包括 <code>VocabularyWordNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void deleteWord(String wordId) {
        if (mapper.deleteWord(id(wordId)) == 0) {
            throw new VocabularyWordNotFoundException();
        }
        mapper.deleteLibraryTopics(id(wordId));
        mapper.deleteLibraryMapping(id(wordId));
        changed("ENGLISH_VOCABULARY_WORD", id(wordId));
    }

    /**
     * changed处理入口。
     * 主要协作调用：<code>events.publishEvent</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void changed(String type, Long id) {
        events.publishEvent(new EnglishSearchContentChangedEvent(type, id));
    }

    /**
     * 必需单词处理入口。
     * 主要协作调用：<code>mapper.word</code>。
     * 实现中的显式失败类型包括 <code>VocabularyWordNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    private Word requiredWord(long wordId) {
        Word word = mapper.word(wordId);
        if (word == null) {
            throw new VocabularyWordNotFoundException();
        }
        attachAudios(List.of(word));
        return word;
    }

    /**
     * 必需主题处理入口。
     * 主要协作调用：<code>mapper.theme</code>。
     * 实现中的显式失败类型包括 <code>VocabularyThemeNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param themeId 主题标识；类型为 <code>Long</code>
     * @return 类型为 <code>Theme</code> 的处理结果
     */
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
    /**
     * 建立关联Audios。
     * 主要协作调用：<code>mapper.audios</code>。
     *
     * @param words words；类型为 <code>List&lt;Word&gt;</code>
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

    /**
     * 公开地址处理入口。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String publicUrl(Long mediaAssetId) {
        return mediaAssetId == null ? null : "/api/media/assets/" + mediaAssetId + "/content";
    }

    /**
     * 从输入构造请求。
     * 关键处理步骤调用 <code>word.setThemeId</code>、<code>id</code>、<code>request.getThemeId</code>、<code>word.setWord</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
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

    /**
     * 校验主题。
     * 关键处理步骤调用 <code>isBlank</code>、<code>request.getLayer</code>、<code>request.getName</code>。
     * 实现中的显式失败类型包括 <code>VocabularyContentInvalidException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ThemeRequest</code>
     */
    private void validateTheme(ThemeRequest request) {
        if (request == null || isBlank(request.getLayer()) || isBlank(request.getName())) {
            throw new VocabularyContentInvalidException("请填写词层和主题名称");
        }
        if (request.getLayer().length() > 100 || request.getName().length() > 200) {
            throw new VocabularyContentInvalidException("词层或主题名称过长");
        }
    }

    /**
     * 校验单词。
     * 关键处理步骤调用 <code>isBlank</code>、<code>request.getThemeId</code>、<code>request.getWord</code>、<code>request.getTranslation</code>。
     * 实现中的显式失败类型包括 <code>VocabularyContentInvalidException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     */
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

    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>isBlank</code>、<code>Long.parseLong</code>、<code>value.trim</code>。
     * 实现中的显式失败类型包括 <code>VocabularyContentInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
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

    /**
     * 数值处理入口。
     *
     * @param value 值；类型为 <code>Integer</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 空白处理入口。
     * 关键处理步骤调用 <code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String empty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 去除首尾空白目标空值处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>trimmed.isEmpty</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 判断是否Blank。
     * 关键处理步骤调用 <code>value.isBlank</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

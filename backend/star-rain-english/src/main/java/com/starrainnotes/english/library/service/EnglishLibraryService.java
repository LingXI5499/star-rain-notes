package com.starrainnotes.english.library.service;

import com.starrainnotes.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.starrainnotes.english.library.dto.LibraryDto.*;
import com.starrainnotes.english.library.mapper.LibraryMapper;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public library navigation and editorial classification; learning identities never change. */
@Service
@RequiredArgsConstructor
public class EnglishLibraryService {
    private static final List<String> STATUSES = List.of("confirmed", "needs-review", "unclassified");
    private final LibraryMapper mapper;
    private final TaxonomyService taxonomy;
    private final VocabularyService vocabulary;
    private final ObjectMapper objectMapper;

    /** Create a current-taxonomy word while retaining the stable identity used by study records. */
    @Transactional
    public com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word createWord(NewWord request) {
        if (request == null || request.getWord() == null || request.getCategory() == null || request.getTopic() == null || request.getTopic().isBlank()) {
            throw new ApiException("ENGLISH_LIBRARY_CLASSIFICATION_INVALID", "请选择一级分类、二级主题并填写单词", 400);
        }
        String code = validatePath(request.getCategory(), request.getTopic());
        if (mapper.lockTopic(code) == null) {
            throw new ApiException("ENGLISH_LIBRARY_TOPIC_INVALID", "词汇主题不存在或已停用", 400);
        }
        Node topic = tree("vocabulary").stream().flatMap(parent -> parent.getChildren().stream()).filter(node -> code.equals(node.getCode())).findFirst().orElseThrow();
        String layer = "library:" + code;
        Long theme = mapper.managedTheme(layer);
        if (theme == null) {
            var source = new com.starrainnotes.english.vocabulary.dto.VocabularyDto.ThemeRequest();
            source.setLayer(layer); source.setName(topic.getName());
            theme = vocabulary.createTheme(source).getId();
        }
        request.getWord().setThemeId(theme.toString());
        var word = vocabulary.createWord(request.getWord());
        Classification classification = new Classification();
        classification.setMigrationStatus("confirmed"); classification.setMigrationReason("");
        classification.setRowVersion(mapper.lock(word.getId()).getRowVersion()); classification.setTopicCodes(List.of(code));
        classify(word.getId(), classification);
        return word;
    }

    @Transactional(readOnly = true)
    public Page<Essay> essays(String query, String tag, String month, String genre, int page, int size) {
        String search = query == null ? "" : query.trim();
        String selectedTag = tag == null ? "" : tag;
        String selectedMonth = month == null ? "" : month;
        String selectedGenre = genre == null ? "" : genre;
        if (search.length() > 100 || selectedTag.length() > 100 || selectedGenre.length() > 100
                || page < 1 || size < 1 || size > 50 || (!selectedMonth.isEmpty() && !selectedMonth.matches("[0-9]{4}-(0[1-9]|1[0-2])"))) {
            throw new ApiException("ENGLISH_LIBRARY_FILTER_INVALID", "随笔筛选参数无效", 400);
        }
        long total = mapper.countEssays(search, selectedTag, selectedMonth, selectedGenre);
        List<Essay> items = mapper.essays(search, selectedTag, selectedMonth, selectedGenre, ((long) page - 1) * size, size);
        for (Essay item : items) {
            try {
                item.setKeywords(item.getKeywordsJson() == null ? List.of() : objectMapper.readValue(item.getKeywordsJson(), new TypeReference<List<String>>() { }));
            } catch (com.fasterxml.jackson.core.JsonProcessingException cause) {
                throw new IllegalStateException("Invalid stored essay keywords", cause);
            }
        }
        return new Page<>(items, total, page, size, (int) Math.ceil((double) total / size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> essayFacets() {
        return Map.of("tags", mapper.essayTags(), "months", mapper.essayMonths(), "genres", mapper.essayGenres(), "total", mapper.overview().essays());
    }

    @Transactional(readOnly = true)
    public Overview overview() { return mapper.overview(); }

    @Transactional(readOnly = true)
    public List<Node> tree(String module) {
        validateModule(module);
        return taxonomy.tree(false).stream().filter(n -> module.equals(n.getLibraryModule())).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts(String module) {
        validateModule(module);
        return mapper.counts(module).stream().collect(Collectors.toMap(Count::code, Count::total));
    }

    @Transactional(readOnly = true)
    public Page<Word> words(String query, String category, String topic, String status, int page, int size) {
        return words(query, category, topic, status, page, size, null);
    }

    @Transactional(readOnly = true)
    public Page<Word> words(String query, String category, String topic, String status, int page, int size, Long theme) {
        String search = query == null ? "" : query.trim();
        if (search.length() > 100 || page < 1 || size < 1 || size > 100 || (theme != null && theme < 1)) {
            throw new ApiException("ENGLISH_LIBRARY_FILTER_INVALID", "搜索或分页参数无效", 400);
        }
        String code = validatePath(category, topic);
        String state = status == null ? "" : status;
        if (!state.isEmpty() && !STATUSES.contains(state)) {
            throw new ApiException("ENGLISH_LIBRARY_STATUS_INVALID", "分类状态无效", 400);
        }
        long total = mapper.countWords(search, code, state, theme);
        List<Word> items = mapper.words(search, code, state, ((long) page - 1) * size, size, theme);
        items.forEach(word -> word.setTopicCodes(mapper.topicCodes(word.getId())));
        Map<Long, com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word> audioWords = vocabulary.wordsByIds(items.stream().map(Word::getId).toList(), 100).stream().collect(Collectors.toMap(com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word::getId, value -> value));
        items.forEach(word -> { if (audioWords.containsKey(word.getId())) word.setAudios(audioWords.get(word.getId()).getAudios()); });
        return new Page<>(items, total, page, size, (int) Math.ceil((double) total / size));
    }

    /** Word edits and classification changes commit atomically with optimistic concurrency. */
    @Transactional
    public void classify(long id, Classification request) {
        if (request == null || request.getMigrationStatus() == null || !STATUSES.contains(request.getMigrationStatus()) || request.getRowVersion() == null
                || request.getTopicCodes() == null || request.getTopicCodes().size() > 20
                || request.getMigrationReason() == null || request.getMigrationReason().length() > 500) {
            throw new ApiException("ENGLISH_LIBRARY_CLASSIFICATION_INVALID", "分类参数无效", 400);
        }
        List<String> codes = request.getTopicCodes().stream().distinct().toList();
        if ("confirmed".equals(request.getMigrationStatus()) == codes.isEmpty()) {
            throw new ApiException("ENGLISH_LIBRARY_CLASSIFICATION_INVALID", "已确认词条必须关联主题；未确认词条不能进入主题目录", 400);
        }
        List<Node> topics = tree("vocabulary").stream().flatMap(n -> n.getChildren().stream()).toList();
        if (codes.stream().anyMatch(code -> topics.stream().noneMatch(t -> Objects.equals(t.getCode(), code)))) {
            throw new ApiException("ENGLISH_LIBRARY_TOPIC_INVALID", "词汇主题不存在或已停用", 400);
        }
        Word word = mapper.lock(id);
        if (word == null) {
            throw new ApiException("ENGLISH_LIBRARY_WORD_NOT_FOUND", "词条不存在", 404);
        }
        if (word.getRowVersion() != request.getRowVersion() || mapper.updateMapping(id, request) != 1) {
            throw new ApiException("ENGLISH_LIBRARY_VERSION_CONFLICT", "词条已被更新，请刷新后重试", 409);
        }
        if (request.getWord() != null) {
            request.getWord().setThemeId(word.getThemeId().toString());
            vocabulary.updateWord(Long.toString(id), request.getWord());
        }
        mapper.clearTopics(id);
        for (String code : codes) {
            Node term = topics.stream().filter(t -> code.equals(t.getCode())).findFirst().orElseThrow();
            mapper.addTopic(id, term.getId());
        }
    }

    private String validatePath(String category, String topic) {
        String parent = category == null ? "" : category.toUpperCase(java.util.Locale.ROOT);
        String child = topic == null ? "" : topic.toUpperCase(java.util.Locale.ROOT);
        if (parent.isEmpty() && child.isEmpty()) { return ""; }
        Node selected = tree("vocabulary").stream().filter(n -> parent.equals(n.getCode())).findFirst()
                .orElseThrow(() -> new ApiException("ENGLISH_LIBRARY_CATEGORY_NOT_FOUND", "词汇分类不存在", 404));
        if (child.isEmpty()) { return parent; }
        if (selected.getChildren().stream().noneMatch(n -> child.equals(n.getCode()))) {
            throw new ApiException("ENGLISH_LIBRARY_TOPIC_NOT_FOUND", "主题不属于当前分类", 404);
        }
        return child;
    }

    private static void validateModule(String module) {
        if (!List.of("vocabulary", "world").contains(module)) {
            throw new ApiException("ENGLISH_LIBRARY_MODULE_INVALID", "模块不存在", 404);
        }
    }
}

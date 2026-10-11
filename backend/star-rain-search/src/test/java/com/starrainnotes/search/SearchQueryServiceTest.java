/*
 * SearchQueryServiceTest：搜索模块的测试文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、EnglishSearchTypes、SearchDocumentMapper、SearchQueryService、SearchQueryServiceImpl、SearchHitVO。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；filtersTypesAndPaginatesWithBoundParameters（筛选Types及Paginates包含BoundParameters处理入口）；rejectsUnknownTypeBeforeQuery（验证拒绝Unknown类型之前查询参数）；englishFilterExpandsToPublicEnglishTypesAndDeduplicatesMixedFilters（英语筛选Expands目标公开英语Types及DeduplicatesMixed筛选处理入口）。
 */

package com.starrainnotes.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.service.SearchQueryService;
import com.starrainnotes.search.service.impl.SearchQueryServiceImpl;
import com.starrainnotes.search.vo.SearchHitVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 搜索查询参数业务服务测试，属于搜索模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SearchQueryServiceTest {
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private SearchDocumentMapper mapper;
    /**
     * 业务服务。
     * 协作类型：<code>SearchQueryService</code>。
     */
    private SearchQueryService service;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     * 关键处理步骤调用 <code>Mockito.mock</code>。
     */
    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(SearchDocumentMapper.class);
        service = new SearchQueryServiceImpl(mapper);
    }

    /**
     * 筛选Types及Paginates包含BoundParameters处理入口。
     * 主要协作调用：<code>mapper.count</code>、<code>mapper.search</code>、<code>service.search</code>。
     */
    @Test
    void filtersTypesAndPaginatesWithBoundParameters() {
        List<String> types = List.of("CHAPTER", "BLOG");
        when(mapper.count("Java", types)).thenReturn(21L);
        when(mapper.search("Java", types, 20L, 20)).thenReturn(List.of(new SearchHitVO()));
        var page = service.search("  Java  ", "CHAPTER,BLOG", 2, 20);
        assertEquals(21, page.getTotal());
        assertEquals(2, page.getPage());
        assertEquals(1, page.getItems().size());
        verify(mapper).search("Java", types, 20L, 20);
    }

    /**
     * 验证拒绝Unknown类型之前查询参数。
     * 主要协作调用：<code>service.search</code>。
     */
    @Test
    void rejectsUnknownTypeBeforeQuery() {
        ApiException exception = assertThrows(ApiException.class,
            () -> service.search("Java", "BLOG,ACCOUNT", 1, 20));
        assertEquals("SEARCH_TYPE_INVALID", exception.getCode());
        Mockito.verifyNoInteractions(mapper);
    }

    /**
     * 英语筛选Expands目标公开英语Types及DeduplicatesMixed筛选处理入口。
     * 主要协作调用：<code>service.search</code>。
     */
    @Test
    void englishFilterExpandsToPublicEnglishTypesAndDeduplicatesMixedFilters() {
        service.search("grammar", "ENGLISH,ENGLISH_READING", 1, 20);
        verify(mapper).count("grammar", EnglishSearchTypes.ALL);
        service.search("词汇", "ENGLISH_VOCABULARY_WORD", 1, 20);
        verify(mapper).count("词汇", List.of("ENGLISH_VOCABULARY_WORD"));
    }

    /**
     * 验证拒绝Blank及Oversized查询参数不含Scanning。
     * 主要协作调用：<code>service.search</code>、<code>service.quick</code>。
     */
    @Test
    void rejectsBlankAndOversizedQueryWithoutScanning() {
        assertEquals("SEARCH_QUERY_REQUIRED", assertThrows(ApiException.class,
            () -> service.search("  ", null, 1, 20)).getCode());
        assertEquals("SEARCH_QUERY_TOO_LONG", assertThrows(ApiException.class,
            () -> service.quick("中".repeat(101), 8)).getCode());
        Mockito.verifyNoInteractions(mapper);
    }

    /**
     * quick及SuggestionsShare查询参数校验处理入口。
     * 主要协作调用：<code>mapper.search</code>、<code>service.quick</code>、<code>service.suggestions</code>。
     */
    @Test
    void quickAndSuggestionsShareQueryValidation() {
        when(mapper.search(eq("中文"), anyList(), eq(0L), eq(8))).thenReturn(List.of());
        service.quick(" 中文 ", 8);
        service.suggestions(" 中文 ", 8);
        verify(mapper).search("中文", List.of(), 0L, 8);
        verify(mapper).suggestions("中文", 8);
    }
}

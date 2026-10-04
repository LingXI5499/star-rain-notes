package com.starrainnotes.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.query.SearchQueryService;
import com.starrainnotes.search.vo.SearchHitVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SearchQueryServiceTest {
    private SearchDocumentMapper mapper;
    private SearchQueryService service;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(SearchDocumentMapper.class);
        service = new SearchQueryService(mapper);
    }

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

    @Test
    void rejectsUnknownTypeBeforeQuery() {
        ApiException exception = assertThrows(ApiException.class,
            () -> service.search("Java", "BLOG,ACCOUNT", 1, 20));
        assertEquals("SEARCH_TYPE_INVALID", exception.getCode());
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void rejectsBlankAndOversizedQueryWithoutScanning() {
        assertEquals("SEARCH_QUERY_REQUIRED", assertThrows(ApiException.class,
            () -> service.search("  ", null, 1, 20)).getCode());
        assertEquals("SEARCH_QUERY_TOO_LONG", assertThrows(ApiException.class,
            () -> service.quick("中".repeat(101), 8)).getCode());
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void quickAndSuggestionsShareQueryValidation() {
        when(mapper.search(eq("中文"), anyList(), eq(0L), eq(8))).thenReturn(List.of());
        service.quick(" 中文 ", 8);
        service.suggestions(" 中文 ", 8);
        verify(mapper).search("中文", List.of(), 0L, 8);
        verify(mapper).suggestions("中文", 8);
    }
}

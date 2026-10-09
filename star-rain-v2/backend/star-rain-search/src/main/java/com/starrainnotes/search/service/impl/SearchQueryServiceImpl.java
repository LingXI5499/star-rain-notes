package com.starrainnotes.search.service.impl;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.search.exception.SearchPageInvalidException;
import com.starrainnotes.search.exception.SearchQueryRequiredException;
import com.starrainnotes.search.exception.SearchQueryTooLongException;
import com.starrainnotes.search.exception.SearchQueryTooShortException;
import com.starrainnotes.search.exception.SearchTypeInvalidException;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.search.service.SearchQueryService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchQueryServiceImpl implements SearchQueryService {
    private final SearchDocumentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SearchHitVO> search(String rawQuery, String rawTypes, int page, int pageSize) {
        String query = query(rawQuery);
        if (page < 1 || pageSize < 1 || pageSize > 100 || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new SearchPageInvalidException();
        }
        List<String> types = types(rawTypes);
        long total = mapper.count(query, types);
        List<SearchHitVO> items = total == 0 ? List.of()
            : mapper.search(query, types, (long) (page - 1) * pageSize, pageSize);
        return PageResult.<SearchHitVO>builder().items(items).total(total).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHitVO> quick(String rawQuery, int limit) {
        if (limit < 1 || limit > 10) throw new SearchPageInvalidException();
        return mapper.search(query(rawQuery), List.of(), 0, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionVO> suggestions(String rawQuery, int limit) {
        if (limit < 1 || limit > 10) throw new SearchPageInvalidException();
        return mapper.suggestions(query(rawQuery), limit);
    }

    private String query(String value) {
        if (value == null || value.isBlank()) throw new SearchQueryRequiredException();
        String query = value.strip();
        int length = query.codePointCount(0, query.length());
        if (length < 2) throw new SearchQueryTooShortException();
        if (length > 100) throw new SearchQueryTooLongException();
        return query;
    }

    private List<String> types(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> types = Arrays.stream(raw.split(",", -1)).map(String::strip).toList();
        if (types.stream().anyMatch(type -> !"ENGLISH".equals(type) && !SearchIndexService.TYPES.contains(type))) {
            throw new SearchTypeInvalidException();
        }
        return types.stream().flatMap(type -> "ENGLISH".equals(type)
            ? EnglishSearchTypes.ALL.stream() : java.util.stream.Stream.of(type)).distinct().toList();
    }

}

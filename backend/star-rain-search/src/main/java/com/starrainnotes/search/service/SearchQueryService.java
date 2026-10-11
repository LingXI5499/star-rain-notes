package com.starrainnotes.search.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.List;

// 搜索查询业务入口：全文检索、快捷检索与搜索建议。
public interface SearchQueryService {

    PageResult<SearchHitVO> search(String rawQuery, String rawTypes, int page, int pageSize);

    List<SearchHitVO> quick(String rawQuery, int limit);

    List<SearchSuggestionVO> suggestions(String rawQuery, int limit);
}

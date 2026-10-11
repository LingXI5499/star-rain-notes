package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchQueryRequiredException extends ApiException {
    public SearchQueryRequiredException() { super("SEARCH_QUERY_REQUIRED", "请输入搜索内容", 400); }
}

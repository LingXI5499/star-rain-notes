package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchQueryTooLongException extends ApiException {
    public SearchQueryTooLongException() { super("SEARCH_QUERY_TOO_LONG", "搜索内容不能超过 100 个字符", 400); }
}

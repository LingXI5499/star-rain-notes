package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchQueryTooShortException extends ApiException {
    public SearchQueryTooShortException() { super("SEARCH_QUERY_TOO_SHORT", "至少输入两个字符", 400); }
}

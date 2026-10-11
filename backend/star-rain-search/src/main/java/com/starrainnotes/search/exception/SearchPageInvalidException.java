package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchPageInvalidException extends ApiException {
    public SearchPageInvalidException() { super("SEARCH_PAGE_INVALID", "分页参数无效", 400); }
}

package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchTypeInvalidException extends ApiException {
    public SearchTypeInvalidException() { super("SEARCH_TYPE_INVALID", "搜索类型无效", 400); }
}

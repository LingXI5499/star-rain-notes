package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

public class SearchRateLimitedException extends ApiException {
    public SearchRateLimitedException() { super("SEARCH_RATE_LIMITED", "搜索过于频繁，请稍后再试", 429); }
}

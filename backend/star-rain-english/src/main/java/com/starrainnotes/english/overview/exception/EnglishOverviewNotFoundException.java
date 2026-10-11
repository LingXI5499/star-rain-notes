package com.starrainnotes.english.overview.exception;

import com.starrainnotes.common.exception.ApiException;

public class EnglishOverviewNotFoundException extends ApiException {
    public EnglishOverviewNotFoundException() {
        super("ENGLISH_OVERVIEW_NOT_FOUND", "英语概览不存在", 404);
    }
}

package com.starrainnotes.english.overview;

import com.starrainnotes.common.exception.ApiException;

public class EnglishOverviewInvalidException extends ApiException {
    public EnglishOverviewInvalidException(String message) {
        super("ENGLISH_OVERVIEW_INVALID", message, 400);
    }
}

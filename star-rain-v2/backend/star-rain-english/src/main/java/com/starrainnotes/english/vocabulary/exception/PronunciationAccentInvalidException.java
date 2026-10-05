package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

public class PronunciationAccentInvalidException extends ApiException {
    public PronunciationAccentInvalidException() {
        super("ENGLISH_PRONUNCIATION_ACCENT_INVALID", "口音只能是 US 或 UK", 400);
    }
}
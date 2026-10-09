package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

public class VocabularyThemeNotFoundException extends ApiException {
    public VocabularyThemeNotFoundException() {
        super("ENGLISH_VOCABULARY_THEME_NOT_FOUND", "词汇主题不存在", 404);
    }
}
package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

public class VocabularyWordNotFoundException extends ApiException {
    public VocabularyWordNotFoundException() {
        super("ENGLISH_VOCABULARY_WORD_NOT_FOUND", "单词不存在", 404);
    }
}
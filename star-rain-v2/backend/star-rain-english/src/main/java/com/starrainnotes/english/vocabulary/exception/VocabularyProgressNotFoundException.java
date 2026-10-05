package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

public class VocabularyProgressNotFoundException extends ApiException {
    public VocabularyProgressNotFoundException() {
        super("ENGLISH_VOCABULARY_PROGRESS_NOT_FOUND", "该单词还没有加入记忆计划", 404);
    }
}
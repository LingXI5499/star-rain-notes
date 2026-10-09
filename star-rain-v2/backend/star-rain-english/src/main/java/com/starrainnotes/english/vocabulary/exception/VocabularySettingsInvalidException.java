package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 学习设置不合法：英文与中文不能同时隐藏，复习方向与每日上限有固定取值区间。
 */
public class VocabularySettingsInvalidException extends ApiException {
    public VocabularySettingsInvalidException(String message) {
        super("ENGLISH_VOCABULARY_SETTINGS_INVALID", message, 400);
    }
}
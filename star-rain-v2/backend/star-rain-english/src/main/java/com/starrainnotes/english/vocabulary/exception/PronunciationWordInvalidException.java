package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 发音接口只接受单个英文单词（字母、内部连字符与撇号，长度 <= 64）。
 */
public class PronunciationWordInvalidException extends ApiException {
    public PronunciationWordInvalidException(String message) {
        super("ENGLISH_PRONUNCIATION_WORD_INVALID", message, 400);
    }
}
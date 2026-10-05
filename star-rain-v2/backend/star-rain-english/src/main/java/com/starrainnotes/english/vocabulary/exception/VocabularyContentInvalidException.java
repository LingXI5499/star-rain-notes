package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 词汇内容（主题 / 单词）写入参数不合法，或路径上的编号无法解析。
 */
public class VocabularyContentInvalidException extends ApiException {
    public VocabularyContentInvalidException(String message) {
        super("ENGLISH_VOCABULARY_CONTENT_INVALID", message, 400);
    }
}

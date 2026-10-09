package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一个 reviewSessionId 被用到另一个单词上：宁可拒绝，也不能把复习记到错的词上。
 */
public class VocabularyReviewSessionConflictException extends ApiException {
    public VocabularyReviewSessionConflictException() {
        super("ENGLISH_VOCABULARY_REVIEW_SESSION_CONFLICT", "本次复习标识已被另一个单词使用，请重新开始当前卡片", 409);
    }
}
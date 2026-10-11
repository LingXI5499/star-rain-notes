package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 上游发音服务不可用（网络失败或非 200）。保留根因，便于区分超时与拒绝。
 */
public class PronunciationUpstreamException extends ApiException {
    public PronunciationUpstreamException(String message) {
        super("ENGLISH_PRONUNCIATION_UPSTREAM", message, 502);
    }

    public PronunciationUpstreamException(String message, Throwable cause) {
        super("ENGLISH_PRONUNCIATION_UPSTREAM", message, 502, cause);
    }
}
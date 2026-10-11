package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 发音提供方被配置关闭时返回 404，前端据此回退浏览器 speechSynthesis。
 */
public class PronunciationDisabledException extends ApiException {
    public PronunciationDisabledException() {
        super("ENGLISH_PRONUNCIATION_DISABLED", "服务端发音未启用", 404);
    }
}
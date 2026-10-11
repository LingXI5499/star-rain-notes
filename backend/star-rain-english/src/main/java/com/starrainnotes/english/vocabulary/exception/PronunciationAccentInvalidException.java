/*
 * PronunciationAccentInvalidException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * PronunciationAccent无效异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>ENGLISH_PRONUNCIATION_ACCENT_INVALID</code>；HTTP 状态：<code>400</code>。
 * 公开提示：口音只能是 US 或 UK。
 */
public class PronunciationAccentInvalidException extends ApiException {
    /**
     * 构造PronunciationAccentInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public PronunciationAccentInvalidException() {
        super("ENGLISH_PRONUNCIATION_ACCENT_INVALID", "口音只能是 US 或 UK", 400);
    }
}
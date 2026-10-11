/*
 * PronunciationDisabledException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 发音提供方被配置关闭时返回 404，前端据此回退浏览器 speechSynthesis。
 */
/**
 * Pronunciation已禁用异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>ENGLISH_PRONUNCIATION_DISABLED</code>；HTTP 状态：<code>404</code>。
 * 公开提示：服务端发音未启用。
 */
public class PronunciationDisabledException extends ApiException {
    /**
     * 构造PronunciationDisabledException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public PronunciationDisabledException() {
        super("ENGLISH_PRONUNCIATION_DISABLED", "服务端发音未启用", 404);
    }
}
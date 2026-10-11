/*
 * PronunciationUpstreamException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 上游发音服务不可用（网络失败或非 200）。保留根因，便于区分超时与拒绝。
 */
/**
 * PronunciationUpstream异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class PronunciationUpstreamException extends ApiException {
    /**
     * 构造PronunciationUpstreamException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public PronunciationUpstreamException(String message) {
        super("ENGLISH_PRONUNCIATION_UPSTREAM", message, 502);
    }

    /**
     * 构造PronunciationUpstreamException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     * @param cause 原始异常，用于保留失败原因；类型为 <code>Throwable</code>
     */
    public PronunciationUpstreamException(String message, Throwable cause) {
        super("ENGLISH_PRONUNCIATION_UPSTREAM", message, 502, cause);
    }
}
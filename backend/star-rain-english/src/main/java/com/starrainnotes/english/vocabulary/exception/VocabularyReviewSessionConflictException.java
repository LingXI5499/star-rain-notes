/*
 * VocabularyReviewSessionConflictException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一个 reviewSessionId 被用到另一个单词上：宁可拒绝，也不能把复习记到错的词上。
 */
/**
 * 词汇审核会话冲突异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>ENGLISH_VOCABULARY_REVIEW_SESSION_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：本次复习标识已被另一个单词使用，请重新开始当前卡片。
 */
public class VocabularyReviewSessionConflictException extends ApiException {
    /**
     * 构造VocabularyReviewSessionConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public VocabularyReviewSessionConflictException() {
        super("ENGLISH_VOCABULARY_REVIEW_SESSION_CONFLICT", "本次复习标识已被另一个单词使用，请重新开始当前卡片", 409);
    }
}
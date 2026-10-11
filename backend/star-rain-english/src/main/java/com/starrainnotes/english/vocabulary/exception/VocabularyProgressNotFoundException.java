/*
 * VocabularyProgressNotFoundException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 词汇进度不存在异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>ENGLISH_VOCABULARY_PROGRESS_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：该单词还没有加入记忆计划。
 */
public class VocabularyProgressNotFoundException extends ApiException {
    /**
     * 构造VocabularyProgressNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public VocabularyProgressNotFoundException() {
        super("ENGLISH_VOCABULARY_PROGRESS_NOT_FOUND", "该单词还没有加入记忆计划", 404);
    }
}
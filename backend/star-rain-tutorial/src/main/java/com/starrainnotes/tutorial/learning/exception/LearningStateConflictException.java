/*
 * LearningStateConflictException：教程与学习模块的业务异常文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 学习状态冲突异常，属于教程与学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class LearningStateConflictException extends ApiException {
    /**
     * 构造LearningStateConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public LearningStateConflictException(String message) {
        super("LEARNING_STATE_CONFLICT", message, 409);
    }
}

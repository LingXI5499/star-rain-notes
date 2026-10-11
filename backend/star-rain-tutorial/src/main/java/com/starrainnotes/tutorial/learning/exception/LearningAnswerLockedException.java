/*
 * LearningAnswerLockedException：教程与学习模块的业务异常文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 学习答案Locked异常，属于教程与学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>LEARNING_ANSWER_LOCKED</code>；HTTP 状态：<code>403</code>。
 * 公开提示：请先提交自己的答案，再查看参考答案。
 */
public class LearningAnswerLockedException extends ApiException {
    /**
     * 构造LearningAnswerLockedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public LearningAnswerLockedException() {
        super("LEARNING_ANSWER_LOCKED", "请先提交自己的答案，再查看参考答案", 403);
    }
}

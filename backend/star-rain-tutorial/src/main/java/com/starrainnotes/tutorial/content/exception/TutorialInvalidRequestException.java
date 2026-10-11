/*
 * TutorialInvalidRequestException：教程与学习模块的业务异常文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.tutorial.content.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 教程无效请求异常，属于教程与学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class TutorialInvalidRequestException extends ApiException {
    /**
     * 构造TutorialInvalidRequestException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public TutorialInvalidRequestException(String message) {
        super("TUTORIAL_INVALID_REQUEST", message, 400);
    }
}

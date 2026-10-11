/*
 * LearningResourceNotFoundException：教程与学习模块的业务异常文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 学习Resource不存在异常，属于教程与学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>LEARNING_RESOURCE_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：学习内容不存在或尚未公开。
 */
public class LearningResourceNotFoundException extends ApiException {
    /**
     * 构造LearningResourceNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public LearningResourceNotFoundException() {
        super("LEARNING_RESOURCE_NOT_FOUND", "学习内容不存在或尚未公开", 404);
    }
}

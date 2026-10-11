/*
 * WorkMediaConflictException：作品模块的业务异常文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 作品媒体冲突异常，属于作品模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>WORK_MEDIA_CONFLICT</code>；HTTP 状态：<code>409</code>。
 * 公开提示：该媒体已关联作品。
 */
public class WorkMediaConflictException extends ApiException {
    /**
     * 构造WorkMediaConflictException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public WorkMediaConflictException() {
        super("WORK_MEDIA_CONFLICT", "该媒体已关联作品", 409);
    }
}

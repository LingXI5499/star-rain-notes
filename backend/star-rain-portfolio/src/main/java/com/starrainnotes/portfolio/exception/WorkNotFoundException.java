/*
 * WorkNotFoundException：作品模块的业务异常文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 作品不存在异常，属于作品模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>WORK_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：作品不存在。
 */
public class WorkNotFoundException extends ApiException {
    /**
     * 构造WorkNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public WorkNotFoundException() {
        super("WORK_NOT_FOUND", "作品不存在", 404);
    }
}

/*
 * AnalyticsContentTypeInvalidException：访问统计模块的业务异常文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.analytics.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 统计内容类型无效异常，属于访问统计模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>ANALYTICS_CONTENT_TYPE_INVALID</code>；HTTP 状态：<code>400</code>。
 * 公开提示：内容类型或数量限制无效。
 */
public class AnalyticsContentTypeInvalidException extends ApiException {
    /**
     * 构造AnalyticsContentTypeInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public AnalyticsContentTypeInvalidException() { super("ANALYTICS_CONTENT_TYPE_INVALID", "内容类型或数量限制无效", 400); }
}

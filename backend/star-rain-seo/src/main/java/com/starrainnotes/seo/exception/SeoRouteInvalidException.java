/*
 * SeoRouteInvalidException：公开页面与 SEO模块的业务异常文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.seo.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * SEO路由无效异常，属于公开页面与 SEO模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>SEO_ROUTE_INVALID</code>；HTTP 状态：<code>400</code>。
 * 公开提示：页面路径无效。
 */
public class SeoRouteInvalidException extends ApiException {
    /**
     * 构造SeoRouteInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public SeoRouteInvalidException() { super("SEO_ROUTE_INVALID", "页面路径无效", 400); }
}

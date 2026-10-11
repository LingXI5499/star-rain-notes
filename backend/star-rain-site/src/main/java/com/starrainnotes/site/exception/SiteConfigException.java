/*
 * SiteConfigException：站点模块的业务异常文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.site.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 站点配置异常，属于站点模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class SiteConfigException extends ApiException {
    /**
     * 构造SiteConfigException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param message 留言；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>int</code>
     */
    public SiteConfigException(String code, String message, int status) {
        super(code, message, status);
    }
}

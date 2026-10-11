/*
 * SearchQueryTooShortException：搜索模块的业务异常文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 搜索查询参数TooShort异常，属于搜索模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>SEARCH_QUERY_TOO_SHORT</code>；HTTP 状态：<code>400</code>。
 * 公开提示：至少输入两个字符。
 */
public class SearchQueryTooShortException extends ApiException {
    /**
     * 构造SearchQueryTooShortException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public SearchQueryTooShortException() { super("SEARCH_QUERY_TOO_SHORT", "至少输入两个字符", 400); }
}

/*
 * SearchPageInvalidException：搜索模块的业务异常文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.search.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 搜索分页无效异常，属于搜索模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>SEARCH_PAGE_INVALID</code>；HTTP 状态：<code>400</code>。
 * 公开提示：分页参数无效。
 */
public class SearchPageInvalidException extends ApiException {
    /**
     * 构造SearchPageInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public SearchPageInvalidException() { super("SEARCH_PAGE_INVALID", "分页参数无效", 400); }
}

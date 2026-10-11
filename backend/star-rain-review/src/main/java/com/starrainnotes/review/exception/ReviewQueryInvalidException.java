/*
 * ReviewQueryInvalidException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核查询参数非法。
 *
 * 错误码 REVIEW_QUERY_INVALID，HTTP 400。
 * 与 Media 模块同一口径：分页越界、状态取值非法等一律明确报错，不静默纠正，
 * 避免前端拿到「看起来正常但被悄悄改了条件」的结果。
 */
/**
 * 审核查询参数无效异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class ReviewQueryInvalidException extends ApiException {

    /**
     * 构造ReviewQueryInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public ReviewQueryInvalidException(String message) {
        super("REVIEW_QUERY_INVALID", message, 400);
    }
}

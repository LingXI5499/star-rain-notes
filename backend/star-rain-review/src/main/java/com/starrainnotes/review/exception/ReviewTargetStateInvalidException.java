/*
 * ReviewTargetStateInvalidException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 目标业务对象当前状态不允许提交审核。
 *
 * 错误码 REVIEW_TARGET_STATE_INVALID，HTTP 400。
 * 例如目标已被归档、已在审核中、或还没有可审核的内容。
 * 具体规则由业务模块判断，业务模块通过这个异常把结论反馈给前端。
 */
/**
 * 审核目标状态无效异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class ReviewTargetStateInvalidException extends ApiException {

    /**
     * 构造ReviewTargetStateInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>、<code>message.isBlank</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public ReviewTargetStateInvalidException(String message) {
        super("REVIEW_TARGET_STATE_INVALID",
                message == null || message.isBlank() ? "目标对象当前状态不允许提交审核" : message,
                400);
    }
}

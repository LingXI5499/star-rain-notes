/*
 * ReviewAlreadyPendingException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一目标 + reviewType 已经存在待审请求。
 *
 * 错误码 REVIEW_ALREADY_PENDING，HTTP 409。
 * 这是并发重复提交与用户重复点击共同的兜底语义：重新提交前必须先取消或等结论。
 */
/**
 * 审核Already待处理异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_ALREADY_PENDING</code>；HTTP 状态：<code>409</code>。
 * 公开提示：该目标已存在待审核请求，不能重复提交。
 */
public class ReviewAlreadyPendingException extends ApiException {

    /**
     * 构造ReviewAlreadyPendingException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewAlreadyPendingException() {
        super("REVIEW_ALREADY_PENDING", "该目标已存在待审核请求，不能重复提交", 409);
    }
}

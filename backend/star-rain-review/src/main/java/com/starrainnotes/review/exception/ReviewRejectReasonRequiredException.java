/*
 * ReviewRejectReasonRequiredException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核拒绝必须填写原因。
 *
 * 错误码 REVIEW_REJECT_REASON_REQUIRED，HTTP 400。
 * 拒绝原因会写进 sr_review_action.note 与 sr_review_request.decision_reason，
 * 是申请人重新提交的唯一依据，因此不能为空。
 */
/**
 * 审核Reject原因必需异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_REJECT_REASON_REQUIRED</code>；HTTP 状态：<code>400</code>。
 * 公开提示：审核拒绝必须填写拒绝原因。
 */
public class ReviewRejectReasonRequiredException extends ApiException {

    /**
     * 构造ReviewRejectReasonRequiredException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewRejectReasonRequiredException() {
        super("REVIEW_REJECT_REASON_REQUIRED", "审核拒绝必须填写拒绝原因", 400);
    }
}

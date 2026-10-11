package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核拒绝必须填写原因。
 *
 * 错误码 REVIEW_REJECT_REASON_REQUIRED，HTTP 400。
 * 拒绝原因会写进 sr_review_action.note 与 sr_review_request.decision_reason，
 * 是申请人重新提交的唯一依据，因此不能为空。
 */
public class ReviewRejectReasonRequiredException extends ApiException {

    public ReviewRejectReasonRequiredException() {
        super("REVIEW_REJECT_REASON_REQUIRED", "审核拒绝必须填写拒绝原因", 400);
    }
}

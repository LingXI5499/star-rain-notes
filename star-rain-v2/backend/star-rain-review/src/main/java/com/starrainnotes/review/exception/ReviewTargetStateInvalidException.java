package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 目标业务对象当前状态不允许提交审核。
 *
 * 错误码 REVIEW_TARGET_STATE_INVALID，HTTP 400。
 * 例如目标已被归档、已在审核中、或还没有可审核的内容。
 * 具体规则由业务模块判断，业务模块通过这个异常把结论反馈给前端。
 */
public class ReviewTargetStateInvalidException extends ApiException {

    public ReviewTargetStateInvalidException(String message) {
        super("REVIEW_TARGET_STATE_INVALID",
                message == null || message.isBlank() ? "目标对象当前状态不允许提交审核" : message,
                400);
    }
}
